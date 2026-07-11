CREATE TABLE wf_template (
    id BIGINT NOT NULL, code VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, business_type VARCHAR(64) NOT NULL, priority INT NOT NULL DEFAULT 0, template_version INT NOT NULL, status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_wf_template (business_type, code, template_version, deleted), KEY idx_wf_template_match (business_type, status, priority, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE wf_template_scope (
    id BIGINT NOT NULL, template_id BIGINT NOT NULL, department_id BIGINT NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_wf_template_scope (template_id, department_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE wf_template_node (
    id BIGINT NOT NULL, template_id BIGINT NOT NULL, node_no INT NOT NULL, node_type VARCHAR(32) NOT NULL, approver_rule JSON NOT NULL, timeout_hours INT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_wf_template_node (template_id, node_no, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE wf_instance (
    id BIGINT NOT NULL, business_type VARCHAR(64) NOT NULL, business_id BIGINT NOT NULL, initiator_user_id BIGINT NOT NULL, template_snapshot JSON NOT NULL, status VARCHAR(32) NOT NULL, current_node_no INT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_wf_instance_business (business_type, business_id, deleted), KEY idx_wf_instance_initiator (initiator_user_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE wf_task (
    id BIGINT NOT NULL, instance_id BIGINT NOT NULL, node_no INT NOT NULL, node_snapshot JSON NOT NULL, assignee_user_id BIGINT NOT NULL, delegated_from_user_id BIGINT NULL, due_time DATETIME(3) NULL, status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), KEY idx_wf_task_assignee (assignee_user_id, status, due_time, deleted), UNIQUE KEY uk_wf_task_pending (instance_id, node_no, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE wf_action_log (
    id BIGINT NOT NULL, instance_id BIGINT NOT NULL, task_id BIGINT NULL, actor_user_id BIGINT NOT NULL, action VARCHAR(32) NOT NULL, comment TEXT NULL, action_snapshot JSON NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id), KEY idx_wf_action_instance (instance_id, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
