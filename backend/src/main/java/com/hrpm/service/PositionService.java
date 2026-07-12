package com.hrpm.service;

import com.hrpm.common.IdGenerator;
import com.hrpm.common.exception.DuplicateResourceException;
import com.hrpm.common.exception.VersionConflictException;
import com.hrpm.dto.CreatePositionDTO;
import com.hrpm.dto.UpdatePositionDTO;
import com.hrpm.entity.Position;
import com.hrpm.mapper.PositionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class PositionService {
    private final PositionMapper mapper;
    private final IdGenerator idGenerator;

    public PositionService(PositionMapper mapper, IdGenerator idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    public List<Position> list() { return mapper.findAll(); }

    @Transactional
    public Position create(CreatePositionDTO request) {
        if (mapper.countByCode(request.code()) > 0) throw new DuplicateResourceException("Position code already exists");
        Position value = new Position(idGenerator.nextId(), request.code(), request.name(), request.jobFamily(),
                request.description(), request.sortNo(), request.status(), 0);
        mapper.insert(value);
        return value;
    }

    @Transactional
    public Position update(long id, UpdatePositionDTO request) {
        Position current = mapper.findById(id);
        if (current == null) throw new VersionConflictException();
        Position value = new Position(id, current.code(), request.name(), request.jobFamily(), request.description(),
                request.sortNo(), request.status(), Integer.parseInt(request.version()));
        if (mapper.update(value) == 0) throw new VersionConflictException();
        return mapper.findById(id);
    }
}
