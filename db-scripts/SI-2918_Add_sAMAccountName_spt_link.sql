-- 1. Add the new custom column
ALTER TABLE [identityiq].[identityiq].[spt_link] ADD sam_account_name NVARCHAR(128);
GO

-- 2. Create the index using your environment's naming convention
CREATE NONCLUSTERED INDEX spt_link_samAccountName_ci ON [identityiq].[identityiq].[spt_link](sam_account_name);
GO