ALTER TABLE public.task_history
DROP CONSTRAINT fk_task_history_task;

ALTER TABLE public.task_history
    ALTER COLUMN task_id DROP NOT NULL;

ALTER TABLE public.task_history
    ADD CONSTRAINT fk_task_history_task
        FOREIGN KEY (task_id)
            REFERENCES public.tasks(id)
            ON DELETE SET NULL;


ALTER TABLE public.task_history
DROP CONSTRAINT chk_task_history_action;

ALTER TABLE public.task_history
    ADD CONSTRAINT chk_task_history_action
        CHECK (
    action IN (
    'CREATED',
    'UPDATED',
    'COMPLETED',
    'REOPENED',
    'CANCELED',
    'ASSIGNED',
    'DELETED'
    )
    );