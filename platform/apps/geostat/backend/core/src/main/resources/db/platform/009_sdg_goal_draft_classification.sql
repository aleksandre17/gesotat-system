/* Run in geostat-system. Draft metadata from verified Kids category codes; publication remains a stewardship decision. */
IF NOT EXISTS (SELECT 1 FROM platform.classification_scheme WHERE scheme_code=N'UN_SDG_GOAL')
  INSERT platform.classification_scheme(scheme_code,title_ka,title_en,standard_reference) VALUES(N'UN_SDG_GOAL',N'გაეროს მდგრადი განვითარების მიზანი',N'UN Sustainable Development Goal',N'United Nations Sustainable Development Goals 2015-2030');
DECLARE @scheme BIGINT=(SELECT scheme_id FROM platform.classification_scheme WHERE scheme_code=N'UN_SDG_GOAL');
IF NOT EXISTS (SELECT 1 FROM platform.classification_version WHERE scheme_id=@scheme AND version=N'2015-2030')
  INSERT platform.classification_version(scheme_id,version,status,valid_from,valid_to) VALUES(@scheme,N'2015-2030','DRAFT','2015-01-01','2030-12-31');
DECLARE @version BIGINT=(SELECT classification_version_id FROM platform.classification_version WHERE scheme_id=@scheme AND version=N'2015-2030');
MERGE platform.classification_item AS target
USING (VALUES
 (N'1',N'არა სიღარიბეს',N'NO POVERTY',1),(N'2',N'არა შიმშილს',N'ZERO HUNGER',2),(N'3',N'ჯანმრთელობა და კეთილდღეობა',N'GOOD HEALTH AND WELL-BEING',3),
 (N'4',N'ხარისხიანი განათლება',N'QUALITY EDUCATION',4),(N'5',N'გენდერული თანასწორობა',N'GENDER EQUALITY',5),(N'6',N'სუფთა წყალი და სანიტარია',N'CLEAN WATER AND SANITATION',6),
 (N'7',N'ხელმისაწვდომი და უსაფრთხო ენერგია',N'AFFORDABLE AND CLEAN ENERGY',7),(N'8',N'ღირსეული სამუშაო და ეკონომიკური ზრდა',N'DECENT WORK AND ECONOMIC GROWTH',8),
 (N'9',N'მრეწველობა, ინოვაცია და ინფრასტრუქტურა',N'INDUSTRY, INNOVATION AND INFRASTRUCTURE',9),(N'10',N'შემცირებული უთანასწორობა',N'REDUCED INEQUALITIES',10),
 (N'11',N'ქალაქებისა და დასახლებების მდგრადი განვითარება',N'SUSTAINABLE CITIES AND COMMUNITIES',11),(N'12',N'გონივრული მოხმარება და წარმოება',N'RESPONSIBLE CONSUMPTION AND PRODUCTION',12),
 (N'13',N'კლიმატის მდგრადობის მიღწევა',N'CLIMATE ACTION',13),(N'14',N'წყალქვეშა რესურსები',N'LIFE BELOW WATER',14),(N'15',N'დედამიწის ეკოსისტემები',N'LIFE ON LAND',15),
 (N'16',N'მშვიდობა, სამართლიანობა, ძლიერი ინსტიტუტები',N'PEACE, JUSTICE AND STRONG INSTITUTIONS',16),(N'17',N'თანამშრომლობა საერთო მიზნებისთვის',N'PARTNERSHIPS FOR THE GOALS',17)
) AS source(code,label_ka,label_en,sort_order)
ON target.classification_version_id=@version AND target.code=source.code
WHEN MATCHED THEN UPDATE SET label_ka=source.label_ka,label_en=source.label_en,sort_order=source.sort_order
WHEN NOT MATCHED THEN INSERT(classification_version_id,code,label_ka,label_en,status,sort_order) VALUES(@version,source.code,source.label_ka,source.label_en,'DRAFT',source.sort_order);
MERGE platform.classification_alias AS target
USING (SELECT classification_item_id,code FROM platform.classification_item WHERE classification_version_id=@version) AS source
ON target.external_system_code=N'KIDS_GOAL_CATEGORY' AND target.external_code=source.code
WHEN MATCHED THEN UPDATE SET classification_item_id=source.classification_item_id
WHEN NOT MATCHED THEN INSERT(classification_item_id,external_system_code,external_code) VALUES(source.classification_item_id,N'KIDS_GOAL_CATEGORY',source.code);
