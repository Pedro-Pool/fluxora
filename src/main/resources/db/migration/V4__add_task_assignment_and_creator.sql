ALTER TABLE public.tasks
    ADD COLUMN assignee_id BIGINT,
    ADD COLUMN created_by_id BIGINT;

UPDATE public.tasks t
SET created_by_id = p.user_id
    FROM public.projects p
WHERE t.project_id = p.id;

ALTER TABLE public.tasks
    ALTER COLUMN created_by_id SET NOT NULL;

ALTER TABLE public.tasks
    ADD CONSTRAINT fk_tasks_assignee
        FOREIGN KEY (assignee_id) REFERENCES public.users(id),
    ADD CONSTRAINT fk_tasks_created_by
        FOREIGN KEY (created_by_id) REFERENCES public.users(id);

CREATE INDEX idx_tasks_assignee_id
    ON public.tasks(assignee_id);

CREATE INDEX idx_tasks_created_by_id
    ON public.tasks(created_by_id);