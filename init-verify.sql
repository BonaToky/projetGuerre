-- init-verify.sql
-- Ce script vérifie que les tables et données ont été correctement importées

DO $$
DECLARE
    table_count INTEGER;
    expected_tables TEXT[] := ARRAY['users', 'articles', 'categories', 'tags', 'article_tags'];
    missing_tables TEXT[] := ARRAY[]::TEXT[];
    t TEXT;
BEGIN
    -- Vérifier chaque table attendue
    FOREACH t IN ARRAY expected_tables
    LOOP
        SELECT COUNT(*) INTO table_count 
        FROM information_schema.tables 
        WHERE table_schema = 'public' 
        AND table_name = t;
        
        IF table_count = 0 THEN
            missing_tables := array_append(missing_tables, t);
        END IF;
    END LOOP;
    
    -- S'il manque des tables, lever une erreur
    IF array_length(missing_tables, 1) > 0 THEN
        RAISE EXCEPTION 'Tables manquantes: %', array_to_string(missing_tables, ', ');
    END IF;
    
    -- Afficher un message de succès
    RAISE NOTICE '✅ Toutes les tables ont été créées avec succès!';
    
    -- Compter les enregistrements dans chaque table
    FOREACH t IN ARRAY expected_tables
    LOOP
        EXECUTE format('SELECT COUNT(*) FROM %I', t) INTO table_count;
        RAISE NOTICE 'Table %: % enregistrements', t, table_count;
    END LOOP;
END $$;