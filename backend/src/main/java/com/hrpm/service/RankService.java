package com.hrpm.service;

import com.hrpm.common.IdGenerator;
import com.hrpm.common.exception.DuplicateResourceException;
import com.hrpm.common.exception.VersionConflictException;
import com.hrpm.dto.CreateRankDTO;
import com.hrpm.dto.UpdateRankDTO;
import com.hrpm.entity.Rank;
import com.hrpm.mapper.RankMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class RankService {
    private final RankMapper mapper;
    private final IdGenerator idGenerator;

    public RankService(RankMapper mapper, IdGenerator idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    public List<Rank> list() { return mapper.findAll(); }

    @Transactional
    public Rank create(CreateRankDTO request) {
        if (mapper.countByCode(request.code()) > 0) throw new DuplicateResourceException("Rank code already exists");
        Rank value = new Rank(idGenerator.nextId(), request.code(), request.name(), request.rankOrder(), request.status(), 0);
        mapper.insert(value);
        return value;
    }

    @Transactional
    public Rank update(long id, UpdateRankDTO request) {
        Rank current = mapper.findById(id);
        if (current == null) throw new VersionConflictException();
        Rank value = new Rank(id, current.code(), request.name(), request.rankOrder(), request.status(), Integer.parseInt(request.version()));
        if (mapper.update(value) == 0) throw new VersionConflictException();
        return mapper.findById(id);
    }
}
