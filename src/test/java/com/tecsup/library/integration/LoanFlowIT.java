package com.tecsup.library.integration;

import com.tecsup.library.model.Book;
import com.tecsup.library.model.Member;
import com.tecsup.library.repo.mem.InMemoryBookRepository;
import com.tecsup.library.repo.mem.InMemoryLoanRepository;
import com.tecsup.library.repo.mem.InMemoryMemberRepository;
import com.tecsup.library.service.DomainException;
import com.tecsup.library.service.LoanService;
import com.tecsup.library.util.ClockProvider;

import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class LoanFlowIT {

    InMemoryBookRepository bookRepo;
    InMemoryMemberRepository memberRepo;
    InMemoryLoanRepository loanRepo;
    LoanService service;

    @BeforeEach
    void setup() {
        Clock fixed = Clock.fixed(
                LocalDate.of(2025, 11, 1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault()
        );
        ClockProvider.setFixedClock(fixed);

        bookRepo = new InMemoryBookRepository();
        memberRepo = new InMemoryMemberRepository();
        loanRepo = new InMemoryLoanRepository();

        bookRepo.save(new Book("ISBN-001", "Clean Code"));
        bookRepo.save(new Book("ISBN-002", "DDD"));
        bookRepo.save(new Book("ISBN-003", "Refactoring"));

        memberRepo.save(new Member("M-01", "Ana"));
        memberRepo.save(new Member("M-02", "Luis"));

        service = new LoanService(bookRepo, memberRepo, loanRepo);
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    @Test
    void flujoExitoso_prestarYDevolverSinMulta() {
        var loan = service.loanBook("ISBN-001", "M-01");
        assertFalse(bookRepo.findByIsbn("ISBN-001").orElseThrow().isAvailable());

        ClockProvider.setFixedClock(
                Clock.offset(ClockProvider.getClock(), java.time.Duration.ofDays(5))
        );

        var fee = service.returnBook(loan.getId());
        assertEquals(BigDecimal.ZERO, fee);

        assertTrue(bookRepo.findByIsbn("ISBN-001").orElseThrow().isAvailable());
    }

    @Test
    void noPermitePrestarLibroYaPrestado_R1() {
        service.loanBook("ISBN-002", "M-01");
        assertThrows(DomainException.class, () -> service.loanBook("ISBN-002", "M-02"));
    }

    @Test
    void noPermiteMasDeTresPrestamosActivos_R2() {
        service.loanBook("ISBN-001", "M-01");
        service.loanBook("ISBN-002", "M-01");
        service.loanBook("ISBN-003", "M-01");

        bookRepo.save(new Book("ISBN-004", "Patterns"));

        assertThrows(DomainException.class, () -> service.loanBook("ISBN-004", "M-01"));
    }

    @Test
    void noPermitePrestarSiHayMora_R3() {
        service.loanBook("ISBN-001", "M-02");

        ClockProvider.setFixedClock(
                Clock.offset(ClockProvider.getClock(), java.time.Duration.ofDays(20))
        );

        assertThrows(DomainException.class, () -> service.loanBook("ISBN-002", "M-02"));
    }

    @Test
    void calculaMultaEnDevolucionTardia_R5() {
        var loan = service.loanBook("ISBN-003", "M-01");

        ClockProvider.setFixedClock(
                Clock.offset(ClockProvider.getClock(), java.time.Duration.ofDays(17))
        );

        var fee = service.returnBook(loan.getId());
        assertEquals(new BigDecimal("4.5"), fee);
    }
}
