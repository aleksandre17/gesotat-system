import React, { useState } from "react";

// -------------------------------------------------------
// ძირითადი მაჩვენებელი — კონტენტი ხელით დაემატება აქ
// -------------------------------------------------------

const MainIndicators = ({ language }) => {
  const [downloaded, setDownloaded] = useState(false);

  return (
    <div className="w-full max-w-screen-2xl mx-auto px-4 py-10">
      <h2
        className="font-bold mb-8"
        style={{
          fontSize: "30px",
          color: "#37496d",
          textAlign: "center",
          fontFeatureSettings: '"case" on',
          fontFamily: "FiraGO",
        }}
      >
        {language === "EN" ? "Main Indicators" : "ძირითადი მაჩვენებელი"}
      </h2>

      <div className="flex justify-center mb-6">
        <svg
          width="70"
          height="1"
          viewBox="0 0 70 1"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          <line y1="0.5" x2="70" y2="0.5" stroke="black" />
        </svg>
      </div>

      {(() => {
        const rows = [
          {
            ka: "18 წლამდე ასაკის მოსახლეობის რიცხოვნობა, კაცი (2025, 1 იანვრის მდგომარეობით)",
            en: "Total population under 18, persons (2025, as of 1 January)",
            value: "842 465",
          },
          {
            ka: "ბავშვების პროცენტული წილი მთლიან მოსახლეობაში, % (2025, 1 იანვრის მდგომარეობით)",
            en: "Percentage of children as a share of the total population, % (2025, as of 1 January)",
            value: "22.7",
          },
          {
            ka: "შობადობის ჯამობრივი კოეფიციენტი (2024)",
            en: "Total fertility rate (2024)",
            value: "1.7",
          },
          {
            ka: "შობადობის ასაკობრივი კოეფიციენტი, 18 წლამდე ასაკის (2024)",
            en: "Age-specific fertility rate under 18 (2024)",
            value: "6.2",
          },
          {
            ka: "ცოცხლად დაბადებულთა რიცხოვნობა, კაცი (2024)",
            en: "Number of live births, persons (2024)",
            value: "39 483",
          },
          {
            ka: "სქესთა რაოდენობრივი თანაფარდობა დაბადებისას (ბიჭი 100 გოგონაზე) (2024)",
            en: "Sex ratio at birth (male births per 100 female births) (2024)",
            value: "110.8",
          },
          {
            ka: "20-24 წლის ასაკის ქალების პროცენტული წილი, რომლებიც პირველად დაქორწინდნენ ან დაიწყეს ცხოვრება პარტნიორთან 18 წლის ასაკამდე, % (2024)",
            en: "Proportion of women aged 20-24 years who were married or in a union before age 18, % (2024)",
            value: "6.9",
          },
          {
            ka: "20-24 წლის ასაკის ქალების პროცენტული წილი, რომლებიც პირველად დაქორწინდნენ ან დაიწყეს ცხოვრება პარტნიორთან 15 წლის ასაკამდე, % (2024)",
            en: "Proportion of women aged 20-24 years who were married or in a union before age 15, % (2024)",
            value: "0.4",
          },
          {
            ka: "ჩვილ ბავშვთა მოკვდაობის კოეფიციენტი (1 000 დაბადებულზე), ‰ (2024)",
            en: "Infant mortality rate (per 1,000 live births), ‰ (2024)",
            value: "7.4",
          },
          {
            ka: "2-17 წლის ბავშვების პროცენტული წილი, ვისაც მინიმუმ ერთ სფეროში აქვს ფუნქციონირების სირთულე, % (2022)",
            en: "Percentage of children aged 2-17 years with functional difficulty in at least one domain, % (2022)",
            value: "5.8",
          },
          {
            ka: "2-4 წლის ბავშვების პროცენტული წილი, ვისაც მინიმუმ ერთ სფეროში აქვს ფუნქციონირების სირთულე, % (2022)",
            en: "Percentage of children aged 2-4 years with functional difficulty in at least one domain,% (2022)",
            value: "2.5",
          },
          {
            ka: "5-17 წლის ბავშვების პროცენტული წილი, ვისაც მინიმუმ ერთ სფეროში აქვს ფუნქციონირების სირთულე, % (2022)",
            en: "Percentage of children aged 5-17 years with functional difficulty in at least one domain, % (2022)",
            value: "6.5",
          },
          {
            ka: "2-7 წლის ასაკის ბავშვების პროცენტული წილი, რომელთაც მომატებული აქვთ სისხლში ტყვიის შემცველობა, % (2018)",
            en: "Percentage of children age 2-7 years with elevated Blood Lead Levels, % (2018)",
            value: "41.1",
          },
          {
            ka: "სიმაღლეში ჩამორჩენის (ზომიერი და მკვეთრი) გავრცელება 5 წლამდე ასაკის ბავშვებში, % (2018)",
            en: "Prevalence of stunting (moderate and severe) among children under 5, % (2018)",
            value: "5.8",
          },
          {
            ka: "გამოფიტვის გავრცელება (ზომიერი და მკვეთრი) 5 წლამდე ასაკის ბავშვებში, % (2018)",
            en: "Prevalence of wasting (moderate and severe) among children under 5, % (2018)",
            value: "0.6",
          },
          {
            ka: "ჭარბწონიანობის გავრცელება (ზომიერი და მკვეთრი) 5 წლამდე ასაკის ბავშვებში, % (2018)",
            en: "Prevalence of overweight (moderate and severe) among children under 5, % (2018)",
            value: "6.0",
          },
          {
            ka: "ამჟამად დაქორწინებული ან პარტნიორთან მცხოვრები 15-49 წლის იმ ქალთა პროპორციული წილი, ვინც სექსუალურ ურთიერთობებთან, კონტრაცეპტივების გამოყენებასთან და რეპროდუქციული ჯანმრთელობის დაცვასთან დაკავშირებით, საკუთარ, ინფორმირებულ გადაწყვეტილებას იღებს, % (2024)",
            en: "Percentage of women age 15-49 years currently married or in union who make their own informed decisions regarding sexual relations, contraceptive use and health care, % (2024)",
            value: "87.9",
          },
          {
            ka: "ამჟამად დაქორწინებული ან პარტნიორთან მცხოვრები 15-19 წლის იმ ქალთა პროპორციული წილი, ვინც სექსუალურ ურთიერთობებთან, კონტრაცეპტივების გამოყენებასთან და რეპროდუქციული ჯანმრთელობის დაცვასთან დაკავშირებით, საკუთარ, ინფორმირებულ გადაწყვეტილებას იღებს, % (2018)",
            en: "Percentage of women age 15-19 years currently married or in union who make their own informed decisions regarding sexual relations, contraceptive use and health care, % (2018)",
            value: "66.3",
          },
          {
            ka: "ქალების პროცენტული წილი, რომლებმაც ორსულობის დროს მიიღეს 4 და მეტი ანტენატალური ვიზიტი, (2024)",
            en: "Antenatal care 4+ visits - percentage of women attended at least four times during pregnancy by any provider (2024)",
            value: "84.0",
          },
          {
            ka: "18 წლამდე ქალების წილი, რომლებმაც მიიღეს პაპილომავირუსის (HPV) ვაქცინის ბოლო დოზა, % (2024)",
            en: "Percentage of females who received the last dose of human papillomavirus (HPV) vaccine per national schedule, % (2024)",
            value: "13.0",
          },
          {
            ka: "DTP ვაქცინის მე-3 დოზა (ჩვილი ბავშვები), % (2023)",
            en: "Third dose of DTP-containing vaccine (infants), % (2023)",
            value: "88.0",
          },
          {
            ka: "PCV ვაქცინის მე-3 დოზა (ჩვილი ბავშვები), % (2023)",
            en: "Third dose of PCV containing vaccine (infants), % (2023)",
            value: "86.0",
          },
          {
            ka: "MCV2 ვაქცინის მე-2 დოზა (18 წლამდე მოსახლეობა) % (2023)",
            en: "MCV2 vaccine second dose (population under 18), % (2023)",
            value: "86.0",
          },
          {
            ka: "მოზარდების (10-19 წლის) მოკვდაობის კოეფიციენტი, ‰ (2024)",
            en: "Adolescent (aged 10-19 years) mortality rate, ‰ (2024)",
            value: "0.32",
          },
          {
            ka: "დედათა მოკვდაობის კოეფიციენტი (100 000 დაბადებულზე), (2024)",
            en: "Maternal mortality ratio (per 100,000 live births), (2024)",
            value: "25.3",
          },
          {
            ka: "კვალიფიციური სამედიცინო პერსონალის მიერ მიღებული მშობიარობების წილი, % (2024)",
            en: "Skilled birth attendant – percentage of deliveries attended by skilled health personnel, % (2024)",
            value: "99.8",
          },
          {
            ka: "5 წლამდე ასაკის ბავშვთა მოკვდაობის კოეფიციენტი, ‰ (2024)",
            en: "Under-five mortality rate, ‰ (2024)",
            value: "8.6",
          },
          {
            ka: "ნეონატალური სიკვდილიანობის მაჩვენებელი (1 000 დაბადებულზე), ‰ (2024)",
            en: "Neonatal mortality rate (per 1000 live births), ‰ (2024)",
            value: "5.0",
          },
          {
            ka: "მოზარდების შობადობის კოეფიციენტები (15-19 წლის ასაკობრივი ჯგუფის ქალების მიერ დაბადებული ბავშვების საშუალო რაოდენობა ამავე ასაკობრივი ჯგუფის 1 000 ქალზე), (2024)",
            en: "Adolescent birth rate (number of live births to women aged 15-19 per 1,000 women in that age group), (2024)",
            value: "17.6",
          },
          {
            ka: "ანემიის გავრცელება 15-24 წლის ორსულ ქალებში, % (2024)",
            en: "Prevalence of anaemia in women aged 15-24 years, by pregnancy status, % (2024)",
            value: "36.2",
          },
          {
            ka: "ანემიის გავრცელება 15-49 წლის ორსულ ქალებში, % (2024)",
            en: "Prevalence of anaemia in women aged 15-49 years, by pregnancy status, % (2024)",
            value: "34.3",
          },
          {
            ka: "36-59 თვის ასაკის ბავშვების პროცენტული წილი, რომლებიც დადიოდნენ საბავშვო ბაღში, % (2018)",
            en: "Percentage of children (aged 36-59 months) attending an early childhood education programme, % (2018)",
            value: "77.9",
          },
          {
            ka: "3-4 წლის ასაკის ბავშვების პროცენტული წილი, რომელთა განვითარება სათანადოდ მიმდინარეობს წერა-კითხვის და დათვლის, ფიზიკური, სოციალურ-ემოციური და სწავლის მიმართულებებში, % (2018)",
            en: "Percentage of children age 3-4 years who are developmentally on track in literacy-numeracy, physical, social-emotional and learning domains, % (2018)",
            value: "89.6",
          },
          {
            ka: "სკოლის მიღმა ყოფნის მაჩვენებელი, სკოლის დაწყებითი საფეხურის შესაბამისი ასაკის ბავშვებისთვის, % (2018)",
            en: "Out-of-school rate for children of primary school age, % (2018)",
            value: "1.2",
          },
          {
            ka: "სკოლის მიღმა ყოფნის მაჩვენებელი, სკოლის არასრული საშუალო (საბაზო) საფეხურის შესაბამისი ასაკის მოზარდებისთვის, % (2018)",
            en: "Out-of-school rate for adolescents of lower secondary school age, % (2018)",
            value: "1.2",
          },
          {
            ka: "სკოლის მიღმა ყოფნის მაჩვენებელი, სკოლის სრული საშუალო საფეხურის შესაბამისი ასაკის ახალგაზრდებისთვის, % (2018)",
            en: "Out-of-school rate for youth of upper secondary school age, % (2018)",
            value: "11.3",
          },
          {
            ka: "სკოლის დაწყებითი საფეხურის დასრულების დონე, % (2018)",
            en: "Primary school completion rate, % (2018)",
            value: "99.9",
          },
          {
            ka: "სკოლის არასრული საშუალო (საბაზო) საფეხურის დასრულების დონე, % (2018)",
            en: "Lower secondary school completion rate, % (2018)",
            value: "97.7",
          },
          {
            ka: "სკოლის სრული საშუალო საფეხურის დასრულების დონე, % (2018)",
            en: "Upper secondary school completion rate, % (2018)",
            value: "80.9",
          },
          {
            ka: "ბავშვების პროცენტული წილი, რომლებიც სასწავლო წლის დასაწყისში დაწყებით სკოლაში შესვლის ოფიციალურ ასაკზე ერთი წლით უმცროსი იყვნენ და დადიან ბაღში ან სკოლის დაწყებით საფეხურზე (დასწრების წმინდა კორექტირებული მაჩვენებელი), % (2018)",
            en: "Percentage of children age one year younger than the official primary school entry age at the beginning of the school year and who are attending kindergarten or primary education (adjusted net attendance ratio), % (2018)",
            value: "89.6",
          },
          {
            ka: "ახალგაზრდების (15-29 წლის) პროცენტული წილი, რომლებიც არ სწავლობენ, არ არიან დასაქმებულები და არ გადიან ტრენინგებს, % (2024)",
            en: "Share of youth (aged 15-29 years) not in education, employment or training, % (2024)",
            value: "24.1",
          },
          {
            ka: "ახალგაზრდების (15-24 წლის) პროცენტული წილი, რომლებიც არ სწავლობენ, არ არიან დასაქმებულები და არ გადიან ტრენინგებს, % (2024)",
            en: "Share of youth (aged 15-24 years) not in education, employment or training, % (2024)",
            value: "17.5",
          },
          {
            ka: "სკოლების უზრუნველყოფა ძირითადი სერვისით (ელექტროენერგია; ინტერნეტი; კომპიუტერები; ადაპტირებული ინფრასტრუქტურა; სასმელი წყალი; საპირფარეშოები; ხელსაბანები (WASH)), % (2024)",
            en: "Providing schools with main services (schools with access to electricity; access to the internet for pedagogical purposes; access to computers; adapted infrastructure and materials for students with disabilities, basic drinking water services, basic hygiene services, basic sanitation services and basic drinking water services (WASH)), % (2024)",
            value: "100.0",
          },
          {
            ka: "0-17 წლის ასაკის მოსახლეობის პროცენტული წილი, რომელიც იღებს საარსებო შემწეობას ან/და სოციალურ პაკეტს, % (2024)",
            en: "Percentage of the population aged 0-17 in the population of the corresponding age covered by subsistence allowance and/or social package, % (2024)",
            value: "34.7",
          },
          {
            ka: "სოციალური პაკეტის მიმღებ 0-17 წლის შშმ პირთა რიცხოვნობა, კაცი (2024)",
            en: "The number of children aged 0-17 with disabilities receiving social package, persons (2024)",
            value: "17 206",
          },
          {
            ka: "0-17 წლის ასაკის ბავშვების პროცენტული წილი, რომლებიც ცხოვრობენ ორივე მშობელთან ერთად, % (2018)",
            en: "Percentage of children aged 0-17 living with two parents, % (2018)",
            value: "81.2",
          },
          {
            ka: "ბავშვების (0-17 წლის) რიცხოვნობა რეზიდენტული მეთვალყურეობის ქვეშ, კაცი (2024)",
            en: "Number of children (aged 0-17) in residential care, persons (2024)",
            value: "289",
          },
          {
            ka: "ბავშვების (0-17 წლის) რიცხოვნობა ოჯახზე დაფუძნებულ ზრუნვაში, კაცი (2024)",
            en: "Number of children (aged 0-17) in family-based care, persons (2024)",
            value: "1 910",
          },
          {
            ka: "სიღარიბის აბსოლუტურ ზღვარს ქვევით მყოფი 0-17 წლის მოსახლეობის წილი, % (2024)",
            en: "Proportion of population aged 0-17 living below the national poverty line, % (2024)",
            value: "12.1",
          },
          {
            ka: "0-17 წლის ბავშვების მატერიალური და სოციალური დანაკლისის ინდექსი, % (2022)",
            en: "Child's aged 0-17 material and social deprivation index, % (2022)",
            value: "37.8",
          },
          {
            ka: "შინამეურნეობებში მცხოვრები 18 წლამდე ასაკის მოსახლეობის პროცენტული წილი, რომლებიც უსაფრთხოდ მართული სასმელი წყლის მომსახურებებით სარგებლობენ, % (2018)",
            en: "Proportion of population aged under 18 using safely managed drinking water service, % (2018)",
            value: "58.4",
          },
          {
            ka: "შინამეურნეობებში მცხოვრები 18 წლამდე ასაკის მოსახლეობის პროცენტული წილი, რომლებიც სარგებლობენ გაუმჯობესებული სანიტარული კვანძებით, % (2018)",
            en: "Proportion of population under 18 using safely managed sanitation services, % (2018)",
            value: "94.1",
          },
          {
            ka: "18 წლამდე ასაკის შინამეურნეობების წევრთა პროცენტული წილი ხელსაბან მოწყობილობასთან ხელმისაწვდომი საპნითა და წყლით, % (2018)",
            en: "Proportion of population under 18 with a handwashing facility with soap and water available at home,% (2018)",
            value: "94.6",
          },
          {
            ka: "18 წლამდე მოსახლეობის პროცენტული წილი, რომლებიც ცხოვრობენ შინამეურნეობებში, სადაც იყენებენ სუფთა საწვავსა და ტექნოლოგიებს, % (2018)",
            en: "Percentage of population under 18 living in households using clean fuels and technologies for cooking, space heating, and lighting, % (2018)",
            value: "61.0",
          },
          {
            ka: "შინამეურნეობებში მცხოვრები 18 წლამდე ასაკის მოსახლეობის პროცენტული წილი, რომლებიც სასმელი წყლის ძირითადი მომსახურებების მომხმარებელია, % (2018)",
            en: "Proportion of population under 18 using basic drinking water services, % (2018)",
            value: "96.6",
          },
          {
            ka: "შინამეურნეობებში მცხოვრები 18 წლამდე ასაკის მოსახლეობის პროცენტული წილი, რომლებიც ძირითადი სანიტარული მომსახურებებით სარგებლობენ, % (2018)",
            en: "Proportion of population under 18 using basic sanitaion services, % (2018)",
            value: "92.6",
          },
          {
            ka: "5-17 წლის ბავშვების პროცენტული წილი, რომლებიც ჩართულები არიან ბავშვთა შრომაში, % (2015)",
            en: "Proportion of children aged 5-17 years engaged in child labour, % (2015)",
            value: "4.2",
          },
          {
            ka: "5-17 წლის ბავშვების მიერ არაანაზღაურებად საოჯახო საქმეებზე დახარჯული დროის წილი, % (2015)",
            en: "Proportion of time spent on unpaid domestic chores and care work by children aged 5-17, % (2015)",
            value: "1.7",
          },
          {
            ka: "6-17 წლის მოსახლეობის წილი, ვინც ფლობს მობილურ ტელეფონს, % (2025)",
            en: "Proportion of individuals aged 6-17 who own a mobile telephone, % (2025)",
            value: "77.5",
          },
          {
            ka: "6-17 წლის ასაკის მოსახლეობის წილი, ვინც გამოიყენა ინტერნეტი ბოლო 3 თვის განმავლობაში, % (2025)",
            en: "Proportion of individuals aged 6-17 years using the Internet within last 3 months, % (2025)",
            value: "97.6",
          },
          {
            ka: "15-24 წლის ასაკის ოდესმე პარტნიორულ ურთიერთობაში მყოფი ქალებისა და გოგონების პროცენტული წილი, რომელთა მიმართ ადგილი ჰქონდა ფიზიკურ, სექსუალურ ან ფსიქოლოგიურ ძალადობას, ბოლო 12 თვის მანძილზე, % (2022)",
            en: "Percentage of ever-partnered women and girls aged 15-24 years subjected to physical, sexual or psychological violence by a current or former intimate partner in the previous 12 months, % (2022)",
            value: "18.8",
          },
          {
            ka: "15-19 წლის ასაკის გოგონების პროცენტული წილი, ვინც ბოლო 12 თვის განმავლობაში განიცადა დისკრიმინაცია ან შევიწროება, % (2018)",
            en: "Percentage of girls aged 15-19 who in the last 12 months have felt discriminated against or harassed, % (2018)",
            value: "7.5",
          },
          {
            ka: "15-19 წლის ასაკის ბიჭების პროცენტული წილი, ვინც ბოლო 12 თვის განმავლობაში განიცადა დისკრიმინაცია ან შევიწროება, % (2018)",
            en: "Percentage of boys aged 15-19 who in the last 12 months have felt discriminated against or harassed, % (2018)",
            value: "4.5",
          },
          {
            ka: "15-29 წლის ქალების პროცენტული წილი, რომელთა მიმართ ბოლო 12 თვის მანძილზე ადგილი ჰქონდა ფიზიკურ, ფსიქოლოგიურ ან სექსუალურ ძალადობას, % (2022)",
            en: "Proportion of women aged 15-29 years subjected to physical, psychological or sexual violence in the previous 12 months, % (2022)",
            value: "18.2",
          },
          {
            ka: "1-14 წლის ასაკის ბავშვების პროცენტული წილი, რომლებმაც განიცადეს ნებისმიერი სახის ფიზიკური დასჯა ან/და ფსიქოლოგიური აგრესია შინამეურნეობის ზრდასრული წევრის მხრიდან, % (2022)",
            en: "Percentage of children aged 1-14 years who experienced any physical punishment and/or psychological aggression by caregivers, % (2022)",
            value: "32.5",
          },
          {
            ka: "ჯანდაცვის წილი მთლიანი ბიუჯეტის ხარჯებში, % (2024)",
            en: "proportion of total government spending on health, % (2024)",
            value: "7.6",
          },
          {
            ka: "სოციალური დაცვის წილი მთლიანი ბიუჯეტის ხარჯებში, % (2024)",
            en: "proportion of total government spending on social protection, % (2024)",
            value: "23.8",
          },
        ];

        const isEN = language === "EN";

        return (
          <div
            style={{
              overflowX: "auto",
              border: "1px solid #eceef2",
              borderRadius: "10px",
              overflow: "hidden",
            }}
          >
            <table
              style={{
                width: "100%",
                borderCollapse: "collapse",
                fontSize: "14px",
                fontFamily: "FiraGO",
              }}
            >
              <thead>
                <tr style={{ backgroundColor: "#e8eefa", color: "#37496d" }}>
                  <th
                    style={{
                      padding: "12px 16px",
                      textAlign: "center",
                      fontWeight: "600",
                      width: "75%",
                      fontSize: "16px",
                    }}
                  >
                    {isEN ? "Indicator" : "მაჩვენებელი"}
                  </th>
                  <th
                    style={{
                      padding: "12px 16px",
                      textAlign: "center",
                      fontWeight: "600",
                      width: "25%",
                      fontSize: "16px",
                    }}
                  >
                    {isEN ? "Data" : "მონაცემი"}
                  </th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row, i) => (
                  <tr
                    key={i}
                    style={{
                      backgroundColor: i % 2 === 0 ? "#f8f9fb" : "#fff",
                      borderBottom: "1px solid #e5e7eb",
                    }}
                  >
                    <td
                      style={{
                        padding: "14px 16px",
                        color: "#333",
                        lineHeight: "1.5",
                      }}
                    >
                      {isEN ? row.en : row.ka}
                    </td>
                    <td
                      style={{
                        padding: "14px 16px",
                        textAlign: "center",
                        fontWeight: "600",
                        color: "#37496d",
                      }}
                    >
                      {row.value}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        );
      })()}

      <div className="flex justify-center gap-3 mt-4 items-center">
        <a
          href={language === "EN" ? "/files/indicators/main-indicators-en.xlsx" : "/files/indicators/main-indicators-ge.xlsx"}
          download={language === "EN" ? "main-indicators-en.xlsx" : "main-indicators-ge.xlsx"}
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "10px",
            background: "#363795",
            borderRadius: "12px",
            cursor: "pointer",
            textDecoration: "none",
          }}
          onClick={() => { setDownloaded(false); setTimeout(() => setDownloaded(true), 500); }}
        >
          <span style={{ fontSize: "14px", color: "#fff", fontFamily: "FiraGO", fontWeight: "500", padding: "8px 16px", fontFeatureSettings: '"case" on' }}>
            {language === "EN" ? "Download" : "გადმოწერა"}
          </span>
          <svg width="50" height="52" viewBox="0 0 50 52" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="50" height="52" rx="12" fill="#040571" />
            <path d="M18.1426 33.7778H31.8574C32.147 33.7779 32.4258 33.8848 32.6373 34.077C32.8489 34.2692 32.9776 34.5323 32.9973 34.8132C33.0171 35.094 32.9264 35.3717 32.7437 35.5901C32.5609 35.8085 32.2997 35.9513 32.0129 35.9896L31.8574 36H18.1426C17.853 35.9999 17.5742 35.893 17.3627 35.7008C17.1511 35.5086 17.0224 35.2455 17.0027 34.9646C16.9829 34.6838 17.0736 34.4061 17.2563 34.1877C17.4391 33.9693 17.7003 33.8265 17.9871 33.7881L18.1426 33.7778ZM24.8446 16.0104L25 16C25.2762 16 25.543 16.0972 25.7512 16.2737C25.9593 16.4502 26.0947 16.694 26.1322 16.96L26.1429 17.1111V28.5007L29.5792 25.1615C29.773 24.9731 30.03 24.8588 30.3033 24.8392C30.5766 24.8196 30.848 24.8961 31.0681 25.0548L31.1961 25.1615C31.3898 25.3499 31.5075 25.5997 31.5276 25.8654C31.5477 26.131 31.469 26.3949 31.3058 26.6089L31.1961 26.7333L25.8077 31.9704C25.6141 32.1585 25.3576 32.273 25.0847 32.2928C24.8117 32.3127 24.5405 32.2366 24.3204 32.0785L24.1923 31.9704L18.8039 26.7333C18.5994 26.5351 18.4797 26.2689 18.4689 25.9883C18.458 25.7078 18.5568 25.4336 18.7454 25.221C18.934 25.0083 19.1984 24.873 19.4855 24.8422C19.7725 24.8114 20.061 24.8873 20.2927 25.0548L20.4208 25.1615L23.8571 28.5037V17.1111C23.8571 16.8426 23.9571 16.5832 24.1387 16.3808C24.3202 16.1785 24.5709 16.0469 24.8446 16.0104Z" fill="white" />
          </svg>
        </a>
        {downloaded && (
          <svg width="50" height="52" viewBox="0 0 50 52" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="50" height="52" rx="15" fill="#388E3C" />
            <path d="M31.9318 21.3551C31.4588 20.8815 30.6907 20.8818 30.2171 21.3551L22.5 29.0725L19.0701 25.6426C18.5965 25.169 17.8288 25.169 17.3552 25.6426C16.8816 26.1162 16.8816 26.8839 17.3552 27.3575L21.6424 31.6447C21.8791 31.8814 22.1894 32 22.4997 32C22.8101 32 23.1207 31.8817 23.3573 31.6447L31.9318 23.07C32.4054 22.5967 32.4054 21.8287 31.9318 21.3551Z" fill="white" />
          </svg>
        )}
      </div>
    </div>
  );
};

export default MainIndicators;
