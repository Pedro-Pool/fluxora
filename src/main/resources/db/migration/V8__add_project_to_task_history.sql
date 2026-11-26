ALTER TABLE public.task_history
    ADD COLUMN project_id BIGINT;

UPDATE public.task_history th
SET project_id = t.project_id
    FROM public.tasks t
WHERE th.task_id = t.id;


DELETE FROM public.task_history
WHERE project_id IS NULL;

ALTER TABLE public.task_history
    ALTER COLUMN project_id SET NOT NULL;

ALTER TABLE public.task_history
    ADD CONSTRAINT fk_task_history_project
        FOREIGN KEY (project_id)
            REFERENCES public.projects(id)
            ON DELETE CASCADE;