ALTER TABLE sys_operation_log
    ADD COLUMN source_address VARCHAR(64) NULL AFTER summary;

CREATE INDEX idx_sys_operation_log_module_object ON sys_operation_log (module, object_id, created_time);
CREATE INDEX idx_sys_operation_log_actor ON sys_operation_log (actor_user_id, created_time);
