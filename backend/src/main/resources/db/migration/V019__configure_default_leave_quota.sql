UPDATE att_leave_type
SET annual_quota = 80.00
WHERE deduct_balance = 1
  AND annual_quota IS NULL
  AND deleted = 0;
