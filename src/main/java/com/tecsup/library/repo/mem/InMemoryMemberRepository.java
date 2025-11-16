package com.tecsup.library.repo.mem;

import com.tecsup.library.model.Member;
import com.tecsup.library.repo.MemberRepository;

import java.util.*;

public class InMemoryMemberRepository implements MemberRepository {

    private final Map<String, Member> data = new HashMap<>();

    @Override
    public Optional<Member> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public void save(Member member) {
        data.put(member.getId(), member);
    }
}
