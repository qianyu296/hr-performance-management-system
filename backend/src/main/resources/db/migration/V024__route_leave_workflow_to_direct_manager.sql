UPDATE wf_template_node n
JOIN wf_template t ON t.id = n.template_id
SET n.node_type = 'DIRECT_MANAGER',
    n.approver_rule = JSON_OBJECT('type', 'DIRECT_MANAGER'),
    n.version = n.version + 1
WHERE t.business_type = 'LEAVE'
  AND t.deleted = 0
  AND n.deleted = 0
  AND n.node_no = 1;