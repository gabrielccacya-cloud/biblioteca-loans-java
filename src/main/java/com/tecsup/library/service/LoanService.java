package com.tecsup.library.service;

import com.tecsup.library.model.Book;
import com.tecsup.library.model.Loan;
import com.tecsup.library.model.Member;
import com.tecsup.library.repo.BookRepository;
import com.tecsup.library.repo.LoanRepository;
import com.tecsup.library.repo.MemberRepository;
import com.tecsup.library.util.ClockProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class LoanService {

    private static final int DEFAULT_DAYS = 14;
    private static final int MAX_ACTIVE_LOANS = 3;
    private static final BigDecimal DAILY_FEE = BigDecimal.valueOf(1.5);

    private final BookRepository bookRepo;
    private final MemberRepository memberRepo;
    private final LoanRepository loanRepo;

    public LoanService(BookRepository bookRepo, MemberRepository memberRepo, LoanRepository loanRepo) {
        this.bookRepo = bookRepo;
        this.memberRepo = memberRepo;
        this.loanRepo = loanRepo;
    }

    public Loan loanBook(String isbn, String memberId) {
        LocalDate today = ClockProvider.today();

        Book book = bookRepo.findByIsbn(isbn)
                .orElseThrow(() -> new DomainException("Libro no existe: " + isbn));

        if (!book.isAvailable())
            throw new DomainException("Libro no disponible: " + isbn);

        loanRepo.findActiveByIsbn(isbn).ifPresent(l -> {
            throw new DomainException("Libro ya prestado (préstamo activo): " + isbn);
        });

        Member member = memberRepo.findById(memberId)
                .orElseThrow(() -> new DomainException("Socio no existe: " + memberId));

        List<Loan> activos = loanRepo.findActiveByMember(memberId);

        if (activos.size() >= MAX_ACTIVE_LOANS)
            throw new DomainException("Máximo de préstamos activos alcanzado");

        boolean hasOverdue = activos.stream().anyMatch(l -> l.isOverdue(today));
        if (hasOverdue)
            throw new DomainException("Socio con préstamo vencido");

        LocalDate due = today.plusDays(DEFAULT_DAYS);
        Loan loan = new Loan(isbn, memberId, today, due);

        book.markBorrowed();
        bookRepo.save(book);

        member.incrementLoans();
        memberRepo.save(member);

        loanRepo.save(loan);

        return loan;
    }

    public BigDecimal returnBook(String loanId) {
        LocalDate today = ClockProvider.today();

        Loan loan = loanRepo.findById(loanId)
                .orElseThrow(() -> new DomainException("Préstamo no existe: " + loanId));

        if (!loan.isActive())
            throw new DomainException("Préstamo ya fue devuelto");

        loan.markReturned(today);
        loanRepo.save(loan);

        Book book = bookRepo.findByIsbn(loan.getIsbn())
                .orElseThrow(() -> new DomainException("Libro no existe"));

        book.markReturned();
        bookRepo.save(book);

        Member member = memberRepo.findById(loan.getMemberId())
                .orElseThrow(() -> new DomainException("Socio no existe"));

        member.decrementLoans();
        memberRepo.save(member);

        if (today.isAfter(loan.getDueDate())) {
            long daysLate = java.time.temporal.ChronoUnit.DAYS.between(loan.getDueDate(), today);
            return DAILY_FEE.multiply(BigDecimal.valueOf(daysLate));
        }

        return BigDecimal.ZERO;
    }
}
