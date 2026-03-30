ALTER TABLE public."session" DROP CONSTRAINT session_material_fk;
ALTER TABLE public."session" DROP COLUMN material_id;


CREATE TABLE public.session_material_block (
	id uuid PRIMARY KEY NOT NULL,
	session_id uuid NULL,
	material_id uuid NULL,
	block_id uuid NULL,
	CONSTRAINT session_material_block_blocks_fk FOREIGN KEY (block_id) REFERENCES public.blocks(block_id),
	CONSTRAINT session_material_block_material_fk FOREIGN KEY (material_id) REFERENCES public.material(material_id),
	CONSTRAINT session_material_block_session_fk FOREIGN KEY (session_id) REFERENCES public."session"(session_id)
);

