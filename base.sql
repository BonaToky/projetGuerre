CREATE DATABASE journale;


CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'lecteur',  -- rôles possibles : lecteur, rédacteur, admin
    created_at TIMESTAMP DEFAULT NOW()
);


CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    parent_id INT REFERENCES categories(id),  -- Pour sous-catégories
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE articles (
    id SERIAL PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    chapeau TEXT,
    slug VARCHAR(255) UNIQUE NOT NULL,
    contenu TEXT NOT NULL,
    image_url VARCHAR(255),
    category_id INT REFERENCES categories(id),
    author_id INT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);


CREATE TABLE tags (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL
);


CREATE TABLE article_tags (
    article_id INT REFERENCES articles(id) ON DELETE CASCADE,
    tag_id INT REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (article_id, tag_id)
);




INSERT INTO users (nom, email, mot_de_passe, role) VALUES
('toky', 'toky@gmail.com', '123', 'admin');


INSERT INTO categories (nom, slug, description, parent_id, created_at) VALUES
('Actualité', 'actualite', 'Dernières nouvelles sur le conflit', NULL, NOW()),
('Analyse', 'analyse', 'Analyses approfondies du conflit', NULL, NOW()),
('Chronologie', 'chronologie', 'Timeline des événements', NULL, NOW()),
('Impacts', 'impacts', 'Conséquences humanitaires, économiques et régionales', NULL, NOW()),
('Moyen-Orient', 'moyen-orient', 'Contexte régional et implications', NULL, NOW());

-- Sous-catégories exemple
INSERT INTO categories (nom, slug, description, parent_id, created_at) VALUES
('Frappes aériennes', 'frappes-aeriennes', 'Détails des opérations militaires aériennes', (SELECT id FROM categories WHERE slug = 'actualite'), NOW());


INSERT INTO tags (nom, slug) VALUES
('Iran', 'iran'),
('Israël', 'israel'),
('États-Unis', 'etats-unis'),
('Trump', 'trump'),
('Frappes', 'frappes'),
('Nucléaire', 'nucleaire'),
('Détroit d''Ormuz', 'detroit-ormuz'),
('Khamenei', 'khamenei'),
('Missiles', 'missiles'),
('Humanitaire', 'humanitaire'),
('Économie', 'economie'),
('Hezbollah', 'hezbollah'),
('Gardiens de la Révolution', 'gardiens-revolution');


INSERT INTO articles (titre, chapeau, slug, contenu, image_url, category_id, author_id, created_at, updated_at) VALUES
('Guerre en Iran : un mois de conflit, bilan et perspectives', 
'La guerre lancée le 28 février 2026 par les États-Unis et Israël entre dans son deuxième mois avec des conséquences majeures.', 
'guerre-iran-un-mois-conflit-bilan', 
'Depuis le 28 février 2026, l''opération conjointe américano-israélienne "Epic Fury" et "Roaring Lion" a visé de nombreuses cibles en Iran, dont des sites nucléaires, des bases militaires et des infrastructures à Téhéran, Isfahan et Qom. Selon les autorités iraniennes, plus de 1 900 morts et 24 000 blessés ont été recensés. Israël et les États-Unis affirment avoir détruit une grande partie des capacités de missiles et de drones iraniens. L''Iran a riposté par des tirs de missiles vers Israël et des bases américaines dans le Golfe, causant des dommages et des blessés. Le détroit d''Ormuz reste une zone de haute tension avec des menaces de blocage du trafic pétrolier.', 
'https://example.com/images/guerre-iran-mois1.jpg', 
(SELECT id FROM categories WHERE slug = 'actualite'), 1, NOW(), NOW()),

('Opération Fureur Épique : les premières frappes américano-israéliennes sur l''Iran', 
'Le 28 février 2026, Donald Trump annonce le lancement d''une vaste offensive contre le régime iranien.', 
'operation-fureur-epique-frappes-iran', 
'Dans la nuit du 28 février, les forces américaines et israéliennes ont lancé des centaines de frappes aériennes sur l''Iran. L''opération visait principalement les installations nucléaires de Natanz, Fordow et Isfahan, ainsi que des sites de production de missiles des Gardiens de la Révolution. Le Guide suprême Ali Khamenei aurait été tué lors des frappes sur Téhéran, selon plusieurs sources. L''Iran a immédiatement riposté avec des salves de missiles balistiques vers Israël, causant des dommages à Tel Aviv et dans le sud du pays.', 
'https://example.com/images/frappes-teheran.jpg', 
(SELECT id FROM categories WHERE slug = 'actualite'), 1, NOW(), NOW()),

('Le rôle de Donald Trump dans l''escalade du conflit avec l''Iran', 
'Le président américain a supervisé personnellement l''opération depuis Mar-a-Lago.', 
'role-trump-escalade-conflit-iran', 
'Donald Trump a justifié l''intervention par la nécessité d''éliminer la menace nucléaire iranienne et de "finir le travail". Il a menacé de frappes plus dures si l''Iran ne reconnaissait pas sa défaite. Des négociations via le Pakistan ont été évoquées, mais l''Iran les a qualifiées d''"injustes". Trump a annoncé que les objectifs militaires pourraient être atteints sans troupes au sol.', 
'https://example.com/images/trump-iran.jpg', 
(SELECT id FROM categories WHERE slug = 'analyse'), 1, NOW(), NOW()),

('Chronologie complète de la guerre en Iran 2026', 
'Des origines aux développements récents : un mois d''affrontements intenses.', 
'chronologie-guerre-iran-2026', 
'13 juin 2025 : Début de la "Guerre des Douze Jours" avec frappes israéliennes sur sites nucléaires iraniens. 28 février 2026 : Lancement de l''opération conjointe US-Israël. Mars 2026 : Ripostes iraniennes massives, frappes sur le Golfe, menaces sur le détroit d''Ormuz. 25-28 mars : Nouvelles salves de missiles iraniens vers Israël et nouvelles frappes israéliennes sur Téhéran. Bilan provisoire : plus de 3 000 morts côté iranien.', 
'https://example.com/images/timeline-iran.jpg', 
(SELECT id FROM categories WHERE slug = 'chronologie'), 1, NOW(), NOW()),

('Impacts humanitaires de la guerre en Iran : plus de 3 millions de déplacés', 
'Le conflit provoque une crise humanitaire majeure en Iran et dans la région.', 
'impacts-humanitaires-guerre-iran', 
'Selon l''ONU et le Croissant-Rouge iranien, plus de 3,2 millions d''Iraniens ont été déplacés. Des écoles, hôpitaux et quartiers résidentiels ont été touchés (ex. : bombardement d''une école à Minab faisant 168 morts). Des infrastructures civiles sont endommagées, aggravant la situation alimentaire et sanitaire.', 
'https://example.com/images/deplaces-iran.jpg', 
(SELECT id FROM categories WHERE slug = 'impacts'), 1, NOW(), NOW()),

('Le détroit d''Ormuz menacé : conséquences économiques mondiales', 
'Les menaces iraniennes de bloquer le détroit font flamber les prix du pétrole.', 
'detroit-ormuz-consequences-economiques', 
'L''Iran a menacé de fermer le détroit d''Ormuz, par lequel transite 20 % du pétrole mondial. Les prix de l''énergie ont flambé de plus de 30 % depuis le début du conflit. L''Union européenne et l''Asie sont particulièrement impactées.', 
'https://example.com/images/ormuz-petrole.jpg', 
(SELECT id FROM categories WHERE slug = 'impacts'), 1, NOW(), NOW()),

('Frappes israéliennes sur Téhéran : les images des destructions', 
'Nouvelles attaques massives dans la capitale iranienne au 28e jour de guerre.', 
'frappes-israeliennes-teheran-destructions', 
'L''armée israélienne a annoncé des frappes de grande envergure sur des infrastructures à Téhéran, causant d''importants dégâts et des coupures d''électricité. L''Iran parle de "crimes de guerre".', 
'https://example.com/images/teheran-frappes.jpg', 
(SELECT id FROM categories WHERE slug = 'actualite'), 1, NOW(), NOW()),

('L''Iran et ses proxies : le rôle du Hezbollah dans le conflit', 
'Le Hezbollah intensifie ses attaques depuis le Liban en soutien à l''Iran.', 
'iran-proxies-hezbollah-role', 
'Le Hezbollah a multiplié les tirs de roquettes vers le nord d''Israël. Israël a répondu par des opérations terrestres au Liban sud. Ce front secondaire complique la situation régionale.', 
'https://example.com/images/hezbollah-liban.jpg', 
(SELECT id FROM categories WHERE slug = 'moyen-orient'), 1, NOW(), NOW()),

('Bilan militaire après un mois : capacités iraniennes affaiblies ?', 
'Les États-Unis affirment avoir détruit les deux tiers des capacités de missiles iraniens.', 
'bilan-militaire-guerre-iran', 
'Selon le Pentagone et Tsahal, une grande partie des usines de missiles et de drones ont été neutralisées. Cependant, l''Iran continue de lancer des attaques sporadiques.', 
'https://example.com/images/bilan-militaire.jpg', 
(SELECT id FROM categories WHERE slug = 'analyse'), 1, NOW(), NOW()),

('Négociations de paix : l''Iran rejette le plan américain en 15 points', 
'Téhéran refuse toute proposition perçue comme une "reddition".', 
'negociations-paix-iran-rejet-plan', 
'Malgré les médiations (Pakistan, etc.), l''Iran maintient qu''il n''a "pas l''intention de négocier" tant que les frappes continuent. Trump parle de progrès tout en menaçant de frappes plus dures.', 
'https://example.com/images/negociations-iran.jpg', 
(SELECT id FROM categories WHERE slug = 'actualite'), 1, NOW(), NOW()),

('Conséquences pour l''Europe : choc énergétique et tensions diplomatiques', 
'L''UE est directement impactée par la flambée des prix et les risques migratoires.', 
'consequences-europe-guerre-iran', 
'Menaces sur le détroit d''Ormuz, hausse des prix de l''énergie, et possible afflux de réfugiés : l''Europe suit de près le conflit.', 
'https://example.com/images/europe-iran.jpg', 
(SELECT id FROM categories WHERE slug = 'impacts'), 1, NOW(), NOW()),

('Sites du patrimoine iranien endommagés par les frappes', 
'L''UNESCO s''inquiète pour des sites historiques touchés.', 
'sites-patrimoine-iran-endommages', 
'Des frappes ont endommagé des zones proches du Palais du Golestan et d''autres sites classés. L''UNESCO appelle à protéger le patrimoine culturel.', 
'https://example.com/images/patrimoine-iran.jpg', 
(SELECT id FROM categories WHERE slug = 'impacts'), 1, NOW(), NOW()),

('Les Gardiens de la Révolution : cible principale des frappes', 
'Décapitation partielle du commandement iranien.', 
'gardiens-revolution-cible-frappes', 
'Plusieurs hauts responsables des IRGC ont été éliminés. Cela a perturbé la chaîne de commandement iranienne.', 
'https://example.com/images/gardiens-revolution.jpg', 
(SELECT id FROM categories WHERE slug = 'actualite'), 1, NOW(), NOW()),

('La guerre de 12 jours de juin 2025 : prélude au conflit actuel', 
'Retour sur le précédent affrontement direct.', 
'guerre-12-jours-2025-prelude', 
'En juin 2025, Israël avait déjà frappé les sites nucléaires iraniens. Les États-Unis étaient intervenus brièvement. Un cessez-le-feu fragile avait tenu jusqu''à la reprise des hostilités fin février 2026.', 
'https://example.com/images/guerre-12-jours.jpg', 
(SELECT id FROM categories WHERE slug = 'chronologie'), 1, NOW(), NOW()),

('Perspectives d''une fin du conflit : mi-avril selon Washington ?', 
'Les États-Unis fixent un calendrier pour atteindre leurs objectifs.', 
'perspectives-fin-conflit-iran', 
'Le secrétaire d''État américain évoque une possible fin mi-avril sans invasion terrestre massive, mais Israël promet d''intensifier ses opérations.', 
'https://example.com/images/fin-conflit.jpg', 
(SELECT id FROM categories WHERE slug = 'analyse'), 1, NOW(), NOW());