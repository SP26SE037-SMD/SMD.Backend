ALTER TABLE public.material ADD id INTEGER NULL;
ALTER TABLE public.material ADD "version" INTEGER NULL;


ALTER TABLE public."session" ADD material_id uuid NULL;
ALTER TABLE public."session" DROP COLUMN "content";
ALTER TABLE public."session" ADD CONSTRAINT session_material_fk FOREIGN KEY (material_id) REFERENCES public.material(material_id);
