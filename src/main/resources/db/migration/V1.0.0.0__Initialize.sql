-- Job
create table job (
  JobKey INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  uuid varchar(32) NOT NULL,
  commandLine varchar(1024) NOT NULL,
  log CLOB NOT NULL
);

create index job_idx_1 on job ( uuid );