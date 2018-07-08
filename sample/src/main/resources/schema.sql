DO $$
BEGIN


  CREATE schema if not exists cluster_tasks;

  IF NOT EXISTS (select * from pg_catalog.pg_user where usename = 'cluster_tasks') THEN
    CREATE role cluster_tasks with login encrypted password 'cluster_tasks_7';
  END IF;

  ALTER schema cluster_tasks owner to cluster_tasks;

  ALTER role cluster_tasks set search_path = cluster_tasks ;


END
$$;

GO


