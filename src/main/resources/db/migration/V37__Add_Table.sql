CREATE TABLE document (
document_id UUID PRIMARY KEY,
document_url text NOT NULL,
major_id UUID, --
status VARCHAR(255),
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT fk_document_major
FOREIGN KEY (major_id)
REFERENCES public.major(major_id)
);