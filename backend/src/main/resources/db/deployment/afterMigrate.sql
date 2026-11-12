-- Default table grants must not let the application alter Flyway's migration history.
REVOKE ALL ON TABLE public.flyway_schema_history FROM csports_app;
