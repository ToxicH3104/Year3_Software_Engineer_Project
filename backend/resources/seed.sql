INSERT OR IGNORE INTO users(userId,universityId,displayName,role) VALUES
('s1','S001','Yiming Chen','student'),('s2','S002','Lin Wang','student'),('s3','S003','Lin Wang','student'),
('o1','O001','Jiaxin Liu','student'),('o2','O002','Zihan Huang','student'),
('t1','T001','Qian Zhou','teacher'),('t2','T002','Min Li','teacher');

-- Existing organiser accounts keep their IDs, sessions and club grants.
UPDATE users SET role='student' WHERE role='organiser';

INSERT OR IGNORE INTO clubs VALUES ('guitar','Guitar Club'),('acm','ACM Club');

INSERT OR IGNORE INTO club_roles VALUES ('guitar','s1','member'),('acm','s2','member'),('guitar','o1','organiser'),('acm','o2','organiser'),('guitar','t1','teacher'),('acm','t2','teacher');

INSERT INTO organiser_grants(clubId,userId,grantedBy,grantedAt,reason)
SELECT cr.clubId,cr.userId,NULL,strftime('%Y-%m-%dT%H:%M:%fZ','now'),'System-provisioned demo club organiser.'
FROM club_roles cr
WHERE cr.relation='organiser' AND (cr.clubId,cr.userId) IN (('guitar','o1'),('acm','o2'))
AND NOT EXISTS (SELECT 1 FROM organiser_grants g WHERE g.clubId=cr.clubId AND g.userId=cr.userId);

INSERT OR IGNORE INTO demo_credentials VALUES
('s1','895ce4458dc915eb7bcf86a06b86e56c','942deb0125b3f37c50a9115e0aec140e9a14930942fc6f26855c30227b3f4ee1'),
('s2','d3999fb7deb5c226c95b4cc7b2715a51','8168d109e3270bfb95f7e6fc6367158f7c25b78a91ba11159db9298533d1560c'),
('s3','b2dc67a62fb4593fc883dd6097d5ae01','5e889acdfbafe61465bd5b619642f8ec73f5ac13867e54779e32852eb44bff4a'),
('o1','71e4f2c3f14bb1dfe40f18c9922bb3c1','54aeb5165e977555a59623ecc43e9627f4a2ad2f75b109b1f3dc044a2624310e'),
('o2','eaf2714b21325ae59b186e44bb31cb2d','d666efb268d6a752d4d754e180676e6e994bb2af7a4419ecd0eb709363e6b67b'),
('t1','07da64bcf6c755c4846eb84b3cbb33e7','c6ce05d391b88456c39342bff1ba45613d347bc61fb7ec7d9ffc6a7b4335a060'),
('t2','2b8ff9793e375f7e590a4e91520722c5','1b2a2c658e6b2e3ff3f78ec32680e597be8e7a1007f5d08b12a20506860dccfc');
