ALTER TABLE public.request ADD task_id uuid NULL;
ALTER TABLE public.request ADD CONSTRAINT request_task_fk FOREIGN KEY (task_id) REFERENCES public.task(task_id);
