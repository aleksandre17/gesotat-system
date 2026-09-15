import React, { useState, useEffect, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import slide1 from "../assets/images/slide-1.jpg";
import slide2 from "../assets/images/slide-2.jpg";
import slide3 from "../assets/images/slide-3.png";
import slide4 from "../assets/images/slide-4.jpg";
import slide5 from "../assets/images/slide-5.png";
import slide6 from "../assets/images/slide-6.jpg";
import slide7 from "../assets/images/slide-7.png";
import slide8 from "../assets/images/slide-8.jpg";
import slide9 from "../assets/images/slide-9.png";
import slide10 from "../assets/images/slide-10.jpg";
import marriageImg from "../assets/images/marriage.png";
import lifeCalcImg from "../assets/images/lifecalculator.png";
import pubGeo1 from "../assets/pubimg/geo/pub2023new.png";
import pubGeo2 from "../assets/pubimg/geo/pub2023.png";
import pubGeo3 from "../assets/pubimg/geo/pub2022.png";
import pubGeo4 from "../assets/pubimg/geo/pub2020.png";
import pubGeo5 from "../assets/pubimg/geo/pub2018.png";
import pubEng1 from "../assets/pubimg/eng/pub2023new.png";
import pubEng2 from "../assets/pubimg/eng/pub2023.jpg";
import pubEng3 from "../assets/pubimg/eng/pub2022.jpg";
import pubEng4 from "../assets/pubimg/eng/pub2020.jpg";
import pubEng5 from "../assets/pubimg/eng/pub2018.jpg";
import pubIcon1 from "../assets/images/publications-1.png";
import pubIcon2 from "../assets/images/publications-2.png";
import pubIcon3 from "../assets/images/publications-3.png";
import pubIcon4 from "../assets/images/publications-4.png";
import linkAdmin from "../assets/images/link-administration.png";
import linkSport from "../assets/images/link-sport.png";
import linkUnicef from "../assets/images/link-unicef.png";
import linkSport2 from "../assets/images/sport.png";

// SDG goal images
const sdgImagesKa = Object.fromEntries(
  Array.from({ length: 17 }, (_, i) => [
    i + 1,
    new URL(`../assets/images/goal-${i + 1}-ka.png`, import.meta.url).href,
  ]),
);
const sdgImagesEn = Object.fromEntries(
  Array.from({ length: 17 }, (_, i) => [
    i + 1,
    new URL(`../assets/images/goal-${i + 1}-en.png`, import.meta.url).href,
  ]),
);

const useWindowWidth = () => {
  const [width, setWidth] = useState(
    typeof window !== "undefined" ? window.innerWidth : 1200,
  );
  useEffect(() => {
    const handler = () => setWidth(window.innerWidth);
    window.addEventListener("resize", handler);
    return () => window.removeEventListener("resize", handler);
  }, []);
  return width;
};

const sdgTitlesKa = [
  "სიღარიბის აღმოფხვრა",
  "შიმშილის აღმოფხვრა",
  "ჯანმრთელობა და კეთილდღეობა",
  "ხარისხიანი განათლება",
  "გენდერული თანასწორობა",
  "სუფთა წყალი და სანიტარია",
  "ხელმისაწვდომი და სუფთა ენერგია",
  "ღირსეული სამუშაო და ეკონომიკური ზრდა",
  "ინდუსტრია, ინოვაცია და ინფრასტრუქტურა",
  "უთანასწორობის შემცირება",
  "მდგრადი ქალაქები და თემები",
  "პასუხისმგებლიანი მოხმარება და წარმოება",
  "კლიმატის ცვლილება",
  "წყლის ქვეშ სიცოცხლე",
  "სიცოცხლე ხმელეთზე",
  "მშვიდობა, მართლმსაჯულება და ძლიერი ინსტიტუტები",
  "პარტნიორობა მიზნების მისაღწევად",
];

const sdgTitlesEn = [
  "No Poverty",
  "Zero Hunger",
  "Good Health and Well-being",
  "Quality Education",
  "Gender Equality",
  "Clean Water and Sanitation",
  "Affordable and Clean Energy",
  "Decent Work and Economic Growth",
  "Industry, Innovation and Infrastructure",
  "Reduced Inequalities",
  "Sustainable Cities and Communities",
  "Responsible Consumption and Production",
  "Climate Action",
  "Life Below Water",
  "Life on Land",
  "Peace, Justice and Strong Institutions",
  "Partnerships for the Goals",
];

const SDGSection = ({ language }) => {
  const [hovered, setHovered] = React.useState(null);
  const navigate = useNavigate();
  const isMobile = useWindowWidth() < 768;
  const images = language === "EN" ? sdgImagesEn : sdgImagesKa;
  const titles = language === "EN" ? sdgTitlesEn : sdgTitlesKa;

  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: isMobile
          ? "repeat(3, 1fr)"
          : "repeat(auto-fill, 150px)",
        gap: isMobile ? "10px" : "30px",
        justifyContent: isMobile ? "stretch" : "space-between",
        maxWidth: isMobile ? "100%" : "1151px",
        margin: isMobile ? "24px auto" : "70px auto",
        padding: isMobile ? "0 8px" : "0",
      }}
    >
      {Array.from({ length: 17 }, (_, i) => (
        <div
          key={i}
          onMouseEnter={() => setHovered(i)}
          onMouseLeave={() => setHovered(null)}
          onClick={() => navigate(`/goals/${i + 1}`)}
          style={{
            cursor: "pointer",
            borderRadius: "10px",
            overflow: "hidden",
            transform: hovered === i ? "scale(1.05)" : "scale(1)",
            transition: "transform 0.2s",
            boxShadow: hovered === i ? "0 4px 16px rgba(0,0,0,0.18)" : "none",
          }}
          title={titles[i]}
        >
          <img
            src={images[i + 1]}
            alt={titles[i]}
            style={{ width: "100%", height: "auto", display: "block" }}
          />
        </div>
      ))}
    </div>
  );
};

const slides = [
  { src: slide1, titleGE: "მოსახლეობა", titleEN: "Population" },
  { src: slide2, titleGE: "ჯამრთელობის\nდაცვა", titleEN: "Health Care" },
  { src: slide3, titleGE: "განათლება", titleEN: "Education" },
  {
    src: slide4,
    titleGE: "სოციალური \nუზრუნველყოფა",
    titleEN: "Social\nSecurity",
  },
  { src: slide5, titleGE: "ცხოვრების\nდონე", titleEN: "Living\nConditions" },
  {
    src: slide6,
    titleGE: "დასაქმება და\nუმუშევრობა",
    titleEN: "Employment and\nUnemployment",
  },
  {
    src: slide7,
    titleGE: "საინფორმაციო და\nსაკომუნიკაციო ტექნოლოგიები",
    titleEN: "ICT",
  },
  { src: slide8, titleGE: "ტურიზმი", titleEN: "Tourism" },
  { src: slide9, titleGE: "სამართალდარღვევები", titleEN: "Crime" },
  {
    src: slide10,
    titleGE: "ეროვნული\nანგარიშები",
    titleEN: "GDP and national Accounts",
  },
];

const t = {
  GE: {
    sections: [
      "სტატისტიკური ინფორმაცია",
      "პუბლიკაციები",
      "მდგრადი განვითარების მიზნები",
      "კანონმდებლობა",
      "ბმულები",
    ],
  },
  EN: {
    sections: [
      "MAIN STATISTICS",
      "PUBLICATIONS",
      "SUSTAINABLE DEVELOPMENT GOALS",
      "LEGISLATION",
      "LINKS",
    ],
  },
};

const SectionTitle = ({ title }) => (
  <div className="w-full flex flex-col items-center py-6">
    <h2
      className="font-firago font-semibold"
      style={{
        fontSize: "18px",
        color: "#1a1a1a",
        fontFeatureSettings: '"case" on',
      }}
    >
      {title}
    </h2>
    <div
      style={{
        width: "60px",
        height: "2px",
        background: "#0066e0",
        marginTop: "8px",
      }}
    />
  </div>
);

const publicationsData = [
  {
    imgGe: pubGeo2,
    imgEn: pubEng2,
    pdfGe:
      "https://www.geostat.ge/media/52967/%E1%83%91%E1%83%90%E1%83%95%E1%83%A8%E1%83%95%E1%83%97%E1%83%90-%E1%83%99%E1%83%94%E1%83%97%E1%83%98%E1%83%9A%E1%83%93%E1%83%A6%E1%83%94%E1%83%9D%E1%83%91%E1%83%98%E1%83%A1-%E1%83%92%E1%83%90%E1%83%9B%E1%83%9D%E1%83%99%E1%83%95%E1%83%9A%E1%83%94%E1%83%95%E1%83%90.pdf",
    pdfEn:
      "https://www.geostat.ge/media/52968/Child-Welfare-Survey-%28CWS%29.pdf",
  },
  {
    imgGe: pubGeo1,
    imgEn: pubEng1,
    pdfGe:
      "https://www.geostat.ge/media/59454/%E1%83%91%E1%83%90%E1%83%95%E1%83%A8%E1%83%95%E1%83%94%E1%83%91%E1%83%98-%E1%83%93%E1%83%90-%E1%83%9B%E1%83%9D%E1%83%96%E1%83%90%E1%83%A0%E1%83%93%E1%83%94%E1%83%91%E1%83%98-%E1%83%A1%E1%83%90%E1%83%A5%E1%83%90%E1%83%A0%E1%83%97%E1%83%95%E1%83%94%E1%83%9A%E1%83%9D%E1%83%A8%E1%83%98-2023.pdf",
    pdfEn:
      "https://www.geostat.ge/media/59455/Children-and-Youth-in-Georgia-2023.pdf",
  },
  {
    imgGe: pubGeo3,
    imgEn: pubEng3,
    pdfGe:
      "https://www.geostat.ge/media/51995/%E1%83%91%E1%83%90%E1%83%95%E1%83%A8%E1%83%95%E1%83%94%E1%83%91%E1%83%98-%E1%83%93%E1%83%90-%E1%83%9B%E1%83%9D%E1%83%96%E1%83%90%E1%83%A0%E1%83%93%E1%83%94%E1%83%91%E1%83%98-%E1%83%A1%E1%83%90%E1%83%A5%E1%83%90%E1%83%A0%E1%83%97%E1%83%95%E1%83%94%E1%83%9A%E1%83%9D%E1%83%A8%E1%83%98-2022.pdf",
    pdfEn:
      "https://www.geostat.ge/media/51996/Children-and-Youth-in-Georgia-2022.pdf",
  },
  {
    imgGe: pubGeo4,
    imgEn: pubEng4,
    pdfGe:
      "https://www.geostat.ge/media/37121/%E1%83%91%E1%83%90%E1%83%95%E1%83%A8%E1%83%95%E1%83%94%E1%83%91%E1%83%98-%E1%83%93%E1%83%90-%E1%83%9B%E1%83%9D%E1%83%96%E1%83%90%E1%83%A0%E1%83%93%E1%83%94%E1%83%91%E1%83%98-%E1%83%A1%E1%83%90%E1%83%A5%E1%83%90%E1%83%A0%E1%83%97%E1%83%95%E1%83%94%E1%83%9A%E1%83%9D%E1%83%A8%E1%83%98-2020.pdf",
    pdfEn:
      "https://www.geostat.ge/media/29405/SFR---2018-Georgia-MICS---Eng.pdf",
  },
  {
    imgGe: pubGeo5,
    imgEn: pubEng5,
    pdfGe:
      "https://www.geostat.ge/media/31157/SFR---2018-Georgia-MICS---Geo.pdf",
    pdfEn:
      "https://www.geostat.ge/media/29405/SFR---2018-Georgia-MICS---Eng.pdf",
  },
];

const Publications = ({ language }) => {
  const [hoveredCard, setHoveredCard] = React.useState(null);
  const isMobile = useWindowWidth() < 768;
  return (
    <div
      style={{
        background: "#1d1c2c",
        height: isMobile ? "auto" : "614px",
        margin: "0",
        position: "relative",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        padding: isMobile ? "50px 16px 32px" : "0 64px",
      }}
    >
      {/* top-left icon */}
      {!isMobile && (
        <img
          src={pubIcon1}
          alt=""
          style={{
            position: "absolute",
            top: "-40px",
            left: 0,
            width: "170px",
            pointerEvents: "none",
            userSelect: "none",
          }}
        />
      )}
      {/* title */}
      <div
        style={{
          position: "absolute",
          top: isMobile ? "12px" : "20px",
          left: isMobile ? "16px" : "180px",
          display: "flex",
          alignItems: "center",
          gap: "12px",
        }}
      >
        <p
          style={{
            color: "#e58800",
            fontFeatureSettings: '"case" on',
            fontFamily: "FiraGO",
            fontSize: isMobile ? "16px" : "20px",
            fontWeight: "bold",
            margin: 0,
          }}
        >
          {language === "EN" ? "PUBLICATIONS" : "პუბლიკაციები"}
        </p>
        <div style={{ height: "2px", background: "#e58800", width: "60px" }} />
      </div>
      {/* bottom-left icon */}
      {!isMobile && (
        <img
          src={pubIcon2}
          alt=""
          style={{
            position: "absolute",
            bottom: 0,
            left: "40px",
            width: "100px",
            pointerEvents: "none",
            userSelect: "none",
          }}
        />
      )}
      {/* top-right icon */}
      {!isMobile && (
        <img
          src={pubIcon3}
          alt=""
          style={{
            position: "absolute",
            top: 0,
            right: 0,
            width: "140px",
            pointerEvents: "none",
            userSelect: "none",
          }}
        />
      )}
      {/* bottom-right icon */}
      {!isMobile && (
        <img
          src={pubIcon4}
          alt=""
          style={{
            position: "absolute",
            bottom: 0,
            right: 0,
            width: "160px",
            pointerEvents: "none",
            userSelect: "none",
          }}
        />
      )}

      {/* Cards container */}
      {isMobile ? (
        /* ── MOBILE: 2-column grid ── */
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(2, 1fr)",
            gap: "12px",
            width: "100%",
            zIndex: 1,
          }}
        >
          {publicationsData.map((pub, i) => (
            <div
              key={i}
              style={{
                borderRadius: "10px",
                border: "3px solid white",
                overflow: "hidden",
                display: "flex",
                flexDirection: "column",
                background: "#f09819",
                position: "relative",
              }}
            >
              <a
                href={language === "EN" ? pub.pdfEn : pub.pdfGe}
                target="_blank"
                rel="noreferrer"
                style={{
                  position: "absolute",
                  top: 0,
                  left: 0,
                  height: "40px",
                  width: "34px",
                  zIndex: 1,
                  display: "flex",
                  justifyContent: "center",
                  alignItems: "center",
                  borderRadius: "0 0 8px 0",
                  background: "rgba(255,255,255,0.9)",
                }}
              >
                <svg
                  width="14"
                  height="18"
                  viewBox="0 0 20 25"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                    fillRule="evenodd"
                    clipRule="evenodd"
                    d="M4.30769 18.75H3.07692V20.3125H4.30769C4.76923 20.3125 5.07692 20 5.07692 19.5312C5.07692 19.0625 4.76923 18.75 4.30769 18.75ZM12.3077 0H0V25H20V7.8125L12.3077 0ZM6.46154 19.6875C6.46154 20.9375 5.53846 21.875 4.30769 21.875H3.07692V23.4375H1.53846V17.1875H4.30769C5.53846 17.1875 6.46154 18.125 6.46154 19.375V19.6875ZM12.7692 20.4688C12.7692 22.0312 11.5385 23.4375 9.84615 23.4375H7.69231V17.1875H9.84615C11.3846 17.1875 12.7692 18.4375 12.7692 20.1562V20.4688ZM18.4615 18.75H15.3846V20.3125H17.6923V21.875H15.3846V23.4375H13.8462V17.1875H18.4615V18.75ZM18.4615 15.625H1.53846V1.5625H12.3077V7.8125H18.4615V15.625ZM9.84615 18.75H9.23077V21.875H9.84615C10.7692 21.875 11.3846 21.25 11.3846 20.3125C11.3846 19.375 10.6154 18.75 9.84615 18.75Z"
                    fill="#E58800"
                  />
                </svg>
              </a>
              <img
                src={language === "EN" ? pub.imgEn : pub.imgGe}
                alt={
                  language === "EN"
                    ? `Publication ${i + 1}`
                    : `პუბლიკაცია ${i + 1}`
                }
                style={{ width: "100%", display: "block" }}
              />
              <div
                style={{
                  display: "flex",
                  justifyContent: "center",
                  alignItems: "center",
                  padding: "10px",
                }}
              >
                <a
                  href={language === "EN" ? pub.pdfEn : pub.pdfGe}
                  target="_blank"
                  rel="noreferrer"
                  style={{
                    color: "white",
                    fontFamily: "FiraGO",
                    fontWeight: "bold",
                    fontSize: "11px",
                    borderRadius: "10px",
                    border: "0.5px solid white",
                    padding: "8px 14px",
                    textDecoration: "none",
                  }}
                >
                  {language === "EN" ? "See more" : "ნახვა"}
                </a>
              </div>
            </div>
          ))}
        </div>
      ) : (
        /* ── DESKTOP: original flex row ── */
        <div
          style={{
            display: "flex",
            gap: "50px",
            alignItems: "center",
            justifyContent: "center",
            width: "100%",
            zIndex: 1,
            position: "relative",
          }}
        >
          {publicationsData.map((pub, i) => (
            <div
              key={i}
              style={{
                width: "280px",
                height: "400px",
                borderRadius: "14px",
                border: "5px solid white",
                overflow: "hidden",
                display: "flex",
                flexWrap: "wrap",
                alignItems: "center",
                background: "#f09819",
                position: "relative",
                flexShrink: 0,
              }}
            >
              {/* PDF icon — top-left absolute */}
              <a
                href={language === "EN" ? pub.pdfEn : pub.pdfGe}
                target="_blank"
                rel="noreferrer"
                style={{
                  position: "absolute",
                  top: 0,
                  left: 0,
                  height: "60px",
                  width: "50px",
                  zIndex: 1,
                  display: "flex",
                  justifyContent: "center",
                  alignItems: "center",
                  borderRadius: "0 0 10px 0",
                  background: "rgba(255,255,255,0.9)",
                  transition: "all 0.3s",
                }}
              >
                <svg
                  width="20"
                  height="25"
                  viewBox="0 0 20 25"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                    fillRule="evenodd"
                    clipRule="evenodd"
                    d="M4.30769 18.75H3.07692V20.3125H4.30769C4.76923 20.3125 5.07692 20 5.07692 19.5312C5.07692 19.0625 4.76923 18.75 4.30769 18.75ZM12.3077 0H0V25H20V7.8125L12.3077 0ZM6.46154 19.6875C6.46154 20.9375 5.53846 21.875 4.30769 21.875H3.07692V23.4375H1.53846V17.1875H4.30769C5.53846 17.1875 6.46154 18.125 6.46154 19.375V19.6875ZM12.7692 20.4688C12.7692 22.0312 11.5385 23.4375 9.84615 23.4375H7.69231V17.1875H9.84615C11.3846 17.1875 12.7692 18.4375 12.7692 20.1562V20.4688ZM18.4615 18.75H15.3846V20.3125H17.6923V21.875H15.3846V23.4375H13.8462V17.1875H18.4615V18.75ZM18.4615 15.625H1.53846V1.5625H12.3077V7.8125H18.4615V15.625ZM9.84615 18.75H9.23077V21.875H9.84615C10.7692 21.875 11.3846 21.25 11.3846 20.3125C11.3846 19.375 10.6154 18.75 9.84615 18.75Z"
                    fill="#E58800"
                  />
                </svg>
              </a>

              {/* Publication image */}
              <img
                src={language === "EN" ? pub.imgEn : pub.imgGe}
                alt={
                  language === "EN"
                    ? `Publication ${i + 1}`
                    : `პუბლიკაცია ${i + 1}`
                }
                style={{ width: "100%" }}
              />

              {/* Bottom bar with View button */}
              <div
                style={{
                  borderRadius: "0 0 14px 14px",
                  height: "90px",
                  display: "flex",
                  flex: 1,
                  justifyContent: "center",
                  alignItems: "center",
                }}
              >
                <a
                  href={language === "EN" ? pub.pdfEn : pub.pdfGe}
                  target="_blank"
                  rel="noreferrer"
                  onMouseEnter={() => setHoveredCard(i)}
                  onMouseLeave={() => setHoveredCard(null)}
                  style={{
                    color: "white",
                    fontFeatureSettings: '"case" on',
                    fontFamily: "FiraGO",
                    fontWeight: "bold",
                    display: "flex",
                    alignItems: "center",
                    gap: hoveredCard === i ? "25px" : "15px",
                    borderRadius: "14px",
                    border:
                      hoveredCard === i
                        ? "0.5px solid #1d1c2c"
                        : "0.5px solid white",
                    padding: "15px 35px",
                    transition: "all 0.3s",
                    textDecoration: "none",
                    background: hoveredCard === i ? "#1d1c2c" : "transparent",
                  }}
                >
                  <span>{language === "EN" ? "See more" : "ნახვა"}</span>
                  <svg
                    width="68"
                    height="15"
                    viewBox="0 0 68 15"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <path
                      d="M0 7.49988H67"
                      stroke="white"
                      strokeMiterlimit="10"
                    />
                    <path
                      d="M60.1169 1L66.8191 7.5L60.1169 14"
                      stroke="white"
                      strokeMiterlimit="10"
                    />
                  </svg>
                </a>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

const legislationData = [
  {
    href: "https://matsne.gov.ge/ka/document/view/1399901?publication=0",
    titleGE: "ბავშვთა უფლებების <br>კონვენცია",
    titleEN: "Children's rights<br>Convention",
    svg: (
      <svg
        width="55"
        height="60"
        viewBox="0 0 55 60"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          fillRule="evenodd"
          clipRule="evenodd"
          d="M0.703128 49.6243C0.31254 49.6243 0 49.3068 0 48.9212V5.29315C0 2.37306 2.37306 0 5.29315 0H44.3312C44.7168 0 45.0343 0.31254 45.0343 0.703128V10.9375C45.0343 11.2623 44.861 11.4729 44.6363 11.57V32.9742C44.8588 33.072 45.0297 33.2815 45.0297 33.6039V43.8383C45.0294 44.2239 44.7172 44.5414 44.3266 44.5414C44.0021 44.5414 43.7261 44.3167 43.6463 44.0158L1.41118 48.6814V48.9212C1.40626 49.3068 1.09372 49.6243 0.703128 49.6243Z"
          fill="#FA3C48"
        />
        <path
          fillRule="evenodd"
          clipRule="evenodd"
          d="M0.703128 49.6243C0.31254 49.6243 0 49.3068 0 48.9212V5.29315C0 2.37306 2.37306 0 5.29315 0H9.61386V47.7754L1.41118 48.6818V48.9215C1.40626 49.3072 1.09372 49.6246 0.703128 49.6246V49.6243Z"
          fill="#CB313B"
        />
        <path
          fillRule="evenodd"
          clipRule="evenodd"
          d="M44.3262 54.6583H20.1903C19.7997 54.6583 19.4872 54.3412 19.4872 53.9552V49.6387H12.2021V53.9552C12.2021 54.3409 11.8895 54.6583 11.499 54.6583H5.76178C2.58294 54.6583 0 52.0754 0 48.8965C0 45.7177 2.58294 43.1348 5.76178 43.1348H44.3312C44.7168 43.1348 45.0343 43.4522 45.0343 43.8379V53.9552C45.0294 54.3409 44.7168 54.6583 44.3262 54.6583Z"
          fill="#FA3C48"
        />
        <path
          fillRule="evenodd"
          clipRule="evenodd"
          d="M43.8625 53.4867V44.3066H5.76177C3.22981 44.3066 1.17175 46.3647 1.17175 48.8967C1.17175 51.4286 3.22981 53.4867 5.76177 53.4867H11.0303V48.467H20.6589V53.4867H43.8625Z"
          fill="#C2E6F7"
        />
        <path
          fillRule="evenodd"
          clipRule="evenodd"
          d="M11.4989 60C11.1199 60 10.7958 59.6857 10.7958 59.2968V48.9355C10.7958 48.5499 11.1083 48.2324 11.4989 48.2324H20.1903C20.5809 48.2324 20.8934 48.5499 20.8934 48.9355V59.2968C20.8934 59.9574 20.063 60.2429 19.6485 59.7461L15.8446 55.2099L12.0407 59.7461C11.9039 59.9121 11.7039 60 11.4986 60H11.4989Z"
          fill="#FFB800"
        />
        <path
          fillRule="evenodd"
          clipRule="evenodd"
          d="M40.2635 49.6387H6.37663C5.45097 49.6387 5.45061 48.2324 6.37663 48.2324L40.2635 48.2328C41.1857 48.2328 41.1917 49.639 40.2635 49.639V49.6387Z"
          fill="#FA3C48"
        />
        <path
          fillRule="evenodd"
          clipRule="evenodd"
          d="M44.3262 34.3066C44.2482 34.3066 44.1701 34.2922 44.0966 34.2676C32.3825 30.1905 33.8134 16.4697 34.0134 14.9217C34.0574 14.5895 34.3309 14.3016 34.6631 14.2773C42.1943 13.7226 43.3671 10.2393 44.3311 10.2393C44.5558 10.2393 44.7657 10.3468 44.8975 10.5272C44.9172 10.5564 47.3831 13.789 53.9893 14.2773C54.3359 14.3016 54.6144 14.58 54.6386 14.9267C54.839 16.4697 56.2794 30.1905 44.5558 34.2676C44.4827 34.2922 44.4046 34.3066 44.3266 34.3066H44.3262Z"
          fill="#62D8B6"
        />
        <path
          fillRule="evenodd"
          clipRule="evenodd"
          d="M43.003 25.1076C42.4907 25.1076 41.4969 23.894 40.8792 23.2767C40.2295 22.627 41.2107 21.6219 41.8752 22.2807L43.0033 23.4089L46.7777 19.6345C47.4295 18.9827 48.429 19.9695 47.7737 20.6305L43.5011 24.903C43.364 25.0398 43.1837 25.108 43.003 25.108V25.1076Z"
          fill="white"
        />
        <path
          fillRule="evenodd"
          clipRule="evenodd"
          d="M28.9651 25.0735H12.7003C11.7736 25.0735 11.7732 23.6672 12.7003 23.6672H28.9651C29.8907 23.6672 29.8907 25.0735 28.9651 25.0735Z"
          fill="#FFB800"
        />
        <path
          fillRule="evenodd"
          clipRule="evenodd"
          d="M32.0799 31.7578H12.695C11.7693 31.7578 11.769 30.3516 12.695 30.3516L32.0799 30.3519C33.002 30.3519 33.008 31.7582 32.0799 31.7582V31.7578Z"
          fill="#FFB800"
        />
        <path
          fillRule="evenodd"
          clipRule="evenodd"
          d="M36.4259 38.4375H12.6954C11.7697 38.4375 11.7693 37.0312 12.6954 37.0312H36.4259C37.3484 37.0312 37.354 38.4375 36.4259 38.4375Z"
          fill="#FFB800"
        />
      </svg>
    ),
  },
  {
    href: "https://matsne.gov.ge/ka/document/view/4613854?publication=4",
    titleGE: "ბავშვის უფლებათა<br>კოდექსი",
    titleEN: "THE CODE ON THE RIGHTS OF THE CHILD",
    svg: (
      <svg
        width="50"
        height="60"
        viewBox="0 0 50 60"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M49.8393 40.7107V57.5016H38.5266V41.8595L49.8393 40.7107Z"
          fill="#808080"
        />
        <path
          d="M46.5167 55.0023C45.1765 52.8904 45.1765 50.1945 46.5167 48.0825L28.561 44.7402H4.58423V57.5016H36.5357L46.5167 55.0023Z"
          fill="#F1F4F6"
        />
        <path
          d="M49.839 2.49902V45.5831L8.45838 46.6141L6.021 46.6842L0.000488284 51.5421V8.45789C0.000371096 3.79441 3.79479 0 8.45838 0H47.3413C48.7211 0 49.839 1.11914 49.839 2.49902Z"
          fill="#808080"
        />
        <path
          d="M8.45813 0V46.6141L6.02075 46.6842L0.000244141 51.5421V8.45789C0.000244141 3.79441 3.79454 0 8.45813 0Z"
          fill="#595959"
        />
        <path
          d="M47.341 60H8.45827C3.79467 60 0.000610352 56.206 0.000610352 51.5424C0.000610352 46.8788 3.79467 43.0847 8.45827 43.0847H47.3408C48.7207 43.0847 49.8393 44.2033 49.8393 45.5832C49.8393 46.963 48.7207 48.0816 47.3408 48.0816H18.6917L14.3619 49.8121L10.0321 48.0816H8.45827C6.54999 48.0816 4.99749 49.6341 4.99749 51.5424C4.99749 53.4507 6.54999 55.0032 8.45827 55.0032H47.3408C48.7207 55.0032 49.8393 56.1217 49.8393 57.5016C49.8393 58.8815 48.7207 60 47.341 60Z"
          fill="#595959"
        />
        <path
          d="M8.45801 4.68042V6.49132C4.23984 6.49132 0.730429 9.24265 0.0785154 12.8342C0.0434763 13.025 0.0169922 13.2181 0 13.4125V8.7078C1.86164 6.27522 4.9582 4.68042 8.45801 4.68042Z"
          fill="#FFE07D"
        />
        <path
          d="M8.45801 8.90942V10.7203C4.01661 10.7203 0.36106 13.7711 0.00012207 17.6414V12.9368C0.0254346 12.9018 0.0520359 12.868 0.0786375 12.8342C1.9486 10.4596 5.00766 8.90942 8.45801 8.90942Z"
          fill="#FFE07D"
        />
        <path
          d="M8.45801 34.0007V35.8116C4.01661 35.8116 0.36106 38.8624 0.00012207 42.7316V38.0281C1.86165 35.5955 4.95821 34.0007 8.45801 34.0007Z"
          fill="#FFE07D"
        />
        <path
          d="M18.6917 48.082V56.2648C18.6917 57.3006 17.4977 57.8824 16.6816 57.2426L15.1291 56.0244C14.6787 55.6707 14.045 55.6707 13.5946 56.0244L12.0421 57.2426C11.2272 57.8824 10.0321 57.3006 10.0321 56.2648V48.082H18.6917Z"
          fill="#DD636E"
        />
        <path
          d="M10.0321 48.082H18.6917V51.542H10.0321V48.082Z"
          fill="#DA4A54"
        />
        <path
          d="M41.5386 25.2172C41.1933 25.2172 40.8634 25.0186 40.7126 24.6832L38.4867 19.7375L36.2609 24.6832C36.0557 25.1393 35.5193 25.3425 35.0637 25.1373C34.6077 24.9321 34.4044 24.3961 34.6097 23.94L37.6611 17.1598C37.8073 16.8349 38.1305 16.626 38.4867 16.626C38.843 16.626 39.1662 16.8349 39.3123 17.1598L42.3638 23.94C42.569 24.396 42.3658 24.9321 41.9098 25.1373C41.7892 25.1914 41.663 25.2172 41.5386 25.2172Z"
          fill="#FFD064"
        />
        <path
          d="M33.6754 24.3115C33.6754 26.9688 35.8296 29.1229 38.4868 29.1229C41.144 29.1229 43.2982 26.9688 43.2982 24.3115H33.6754Z"
          fill="#FFE07D"
        />
        <path
          d="M40.1721 28.8201C39.647 29.0157 39.0796 29.1232 38.4869 29.1232C35.8298 29.1232 33.6748 26.9695 33.6748 24.3123H37.0455C37.0454 26.3766 38.3455 28.1368 40.1721 28.8201Z"
          fill="#FFD064"
        />
        <path
          d="M23.0334 25.2172C22.688 25.2172 22.3582 25.0186 22.2073 24.6832L19.9815 19.7375L17.7556 24.6832C17.5503 25.1393 17.0141 25.3425 16.5584 25.1373C16.1025 24.9321 15.8991 24.3961 16.1045 23.94L19.1559 17.1598C19.302 16.8349 19.6252 16.626 19.9815 16.626C20.3377 16.626 20.6609 16.8349 20.8071 17.1598L23.8585 23.94C24.0638 24.396 23.8605 24.9321 23.4045 25.1373C23.2839 25.1914 23.1577 25.2172 23.0334 25.2172Z"
          fill="#FFD064"
        />
        <path
          d="M15.1702 24.3115C15.1702 26.9688 17.3243 29.1229 19.9815 29.1229C22.6388 29.1229 24.7929 26.9688 24.7929 24.3115H15.1702Z"
          fill="#FFE07D"
        />
        <path
          d="M21.6669 28.8201C21.1418 29.0157 20.5744 29.1232 19.9816 29.1232C17.3245 29.1232 15.1696 26.9695 15.1696 24.3123H18.5402C18.5401 26.3766 19.8403 28.1368 21.6669 28.8201Z"
          fill="#FFD064"
        />
        <path
          d="M31.1486 33.0891H27.3195V9.49811C27.3195 8.44072 28.1767 7.5835 29.2341 7.5835C30.2915 7.5835 31.1487 8.44072 31.1487 9.49811L31.1486 33.0891Z"
          fill="#FFE07D"
        />
        <path
          d="M30.1915 7.84072C29.6193 8.17025 29.2342 8.78959 29.2342 9.49822V33.089H27.3195V9.49822C27.3195 8.44072 28.1766 7.5835 29.2342 7.5835C29.5831 7.5835 29.9101 7.67654 30.1915 7.84072Z"
          fill="#FFD064"
        />
        <path
          d="M38.4867 12.4492H19.9812C18.5778 12.4492 17.4402 13.5869 17.4402 14.9902C17.4402 16.3935 18.5778 17.5312 19.9812 17.5312H24.152C24.152 20.3378 26.4272 22.6131 29.2339 22.6131C32.0406 22.6131 34.3159 20.3379 34.3159 17.5312H38.4867C39.89 17.5312 41.0277 16.3935 41.0277 14.9902C41.0277 13.5869 39.8901 12.4492 38.4867 12.4492Z"
          fill="#FFE07D"
        />
        <path
          d="M19.5855 14.9902C19.5855 15.6916 19.8704 16.3266 20.3292 16.7866C20.7892 17.2465 21.4242 17.5302 22.1256 17.5302H19.9815C19.2802 17.5302 18.6451 17.2465 18.1852 16.7866C17.7252 16.3266 17.4403 15.6916 17.4403 14.9902C17.4403 13.5862 18.5788 12.449 19.9815 12.449H22.1256C20.7227 12.449 19.5855 13.5862 19.5855 14.9902Z"
          fill="#FFD064"
        />
        <path
          d="M30.307 22.4994C29.9618 22.5743 29.6032 22.6128 29.235 22.6128C26.4281 22.6128 24.1525 20.3371 24.1525 17.5303H26.2965C26.2966 19.9689 28.0157 22.008 30.307 22.4994Z"
          fill="#FFD064"
        />
        <path
          d="M29.234 19.3037C30.2275 19.3037 31.0329 18.4983 31.0329 17.5048C31.0329 16.5112 30.2275 15.7058 29.234 15.7058C28.2405 15.7058 27.4351 16.5112 27.4351 17.5048C27.4351 18.4983 28.2405 19.3037 29.234 19.3037Z"
          fill="#FFD064"
        />
        <path
          d="M35.434 34.9945H23.034C21.9817 34.9945 21.1285 34.1414 21.1285 33.0891C21.1285 32.0367 21.9817 31.1836 23.034 31.1836H35.4339C36.4862 31.1836 37.3393 32.0367 37.3393 33.0891C37.3394 34.1414 36.4863 34.9945 35.434 34.9945Z"
          fill="#FFE07D"
        />
        <path
          d="M27.3302 34.9954H23.0348C21.9821 34.9954 21.1285 34.1419 21.1285 33.0891C21.1285 32.5627 21.3422 32.087 21.6875 31.7418C22.0316 31.3965 22.5084 31.1841 23.0348 31.1841H27.3302C26.8038 31.1841 26.327 31.3965 25.9829 31.7418C25.6377 32.087 25.4239 32.5627 25.4239 33.0891C25.4239 34.1419 26.2774 34.9954 27.3302 34.9954Z"
          fill="#FFD064"
        />
      </svg>
    ),
  },
  {
    href: "https://matsne.gov.ge/ka/document/view/4936402?publication=0",
    titleGE:
      "საქართველოს ახალგაზრდული<br>პოლიტიკის კონცეფცია 2020–2030<br>წლებისთვის",
    titleEN: "Georgian Youth Policy Concept 2020-2030 for years",
    svg: (
      <svg
        width="60"
        height="60"
        viewBox="0 0 60 60"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M43.4483 42.7241V57.931C43.4449 59.0723 42.5206 59.9966 41.3793 60H2.06897C0.927722 59.9966 0.00340841 59.0723 0 57.931V7.24138L7.24138 0H41.3793C42.5206 0.00340841 43.4449 0.927722 43.4483 2.06897V42.7241Z"
          fill="#ECF0F1"
        />
        <path
          d="M0 7.24138H5.17241C6.31507 7.24138 7.24138 6.31507 7.24138 5.17241V0L0 7.24138Z"
          fill="#D1D4D1"
        />
        <path
          d="M19.6045 24.5855L9.6259 32.6369C9.25458 32.9402 8.78946 33.1051 8.31003 33.1034C7.66982 33.1037 7.0648 32.8106 6.66831 32.3079C5.94299 31.3925 6.0887 30.064 6.99521 29.3276L16.9521 21.2969L19.6045 24.5855Z"
          fill="#A56A43"
        />
        <path
          d="M18.6259 16.0894L16.7203 17.6276C16.2825 17.983 16.0039 18.4978 15.9457 19.0587C15.8875 19.6196 16.0545 20.1806 16.41 20.6183L19.9779 25.0418C20.3357 25.4857 20.8588 25.7647 21.4268 25.8147C21.9947 25.8646 22.5585 25.6812 22.9883 25.3066L24.7241 23.8004L18.6259 16.0894Z"
          fill="#805333"
        />
        <path
          d="M34.5321 19.1885L26.3018 25.801L16.9221 13.9437L25.1525 7.3313L34.5321 19.1885Z"
          fill="#A56A43"
        />
        <path
          d="M22.9221 4.3636L14.5427 11.0877C14.1 11.444 14.0275 12.0906 14.3803 12.536L15.4034 13.8291C15.5718 14.0434 15.819 14.1813 16.0898 14.212C16.3606 14.2427 16.6324 14.1637 16.8445 13.9926L25.2186 7.26842C25.6613 6.9122 25.7338 6.26558 25.381 5.82015L24.3579 4.52704C24.1895 4.31454 23.9434 4.17784 23.6739 4.14717C23.4045 4.1165 23.134 4.19438 22.9221 4.3636Z"
          fill="#CB8252"
        />
        <path
          d="M34.5517 19.172L26.1724 25.8961C25.7297 26.2524 25.6572 26.899 26.01 27.3444L27.0331 28.6386C27.2018 28.8527 27.4492 28.9904 27.72 29.0211C27.9909 29.0518 28.2628 28.9729 28.4751 28.802L36.8482 22.0779C37.2946 21.7216 37.3678 21.071 37.0117 20.6244L35.9886 19.3313C35.8192 19.1196 35.5726 18.9839 35.3032 18.954C35.0337 18.9241 34.7633 19.0025 34.5517 19.172Z"
          fill="#CB8252"
        />
        <path
          d="M29.9999 39.3104H6.20685C5.63552 39.3104 5.17236 38.8473 5.17236 38.2759C5.17236 37.7046 5.63552 37.2415 6.20685 37.2415H29.9999C30.5713 37.2415 31.0344 37.7046 31.0344 38.2759C31.0344 38.8473 30.5713 39.3104 29.9999 39.3104Z"
          fill="#547580"
        />
        <path
          d="M24.8275 44.4828H6.20685C5.63552 44.4828 5.17236 44.0196 5.17236 43.4483C5.17236 42.877 5.63552 42.4138 6.20685 42.4138H24.8275C25.3989 42.4138 25.862 42.877 25.862 43.4483C25.862 44.0196 25.3989 44.4828 24.8275 44.4828Z"
          fill="#547580"
        />
        <path
          d="M21.7241 49.6551H6.20685C5.63552 49.6551 5.17236 49.192 5.17236 48.6207C5.17236 48.0493 5.63552 47.5862 6.20685 47.5862H21.7241C22.2954 47.5862 22.7586 48.0493 22.7586 48.6207C22.7586 49.192 22.2954 49.6551 21.7241 49.6551Z"
          fill="#547580"
        />
        <path
          d="M26.941 54.8278H6.16241C5.59108 54.8278 5.12793 54.3646 5.12793 53.7933C5.12793 53.2219 5.59108 52.7588 6.16241 52.7588H26.941C27.5124 52.7588 27.9755 53.2219 27.9755 53.7933C27.9755 54.3646 27.5124 54.8278 26.941 54.8278Z"
          fill="#547580"
        />
        <path
          d="M34.9241 52.2207C34.8029 52.2951 34.6747 52.3575 34.5414 52.407L30.5069 53.9173L28.2517 54.7552C27.7838 54.918 27.264 54.7988 26.9136 54.4485C26.5633 54.0982 26.4441 53.5784 26.6069 53.1104L27.5069 50.7001L28.9552 46.8207C29.0047 46.6874 29.067 46.5592 29.1414 46.438L32.0379 49.3345L34.9241 52.2207Z"
          fill="#FDD7AD"
        />
        <path
          d="M51.0517 30.3105L32.0379 49.3336L29.1414 46.437C29.2232 46.3084 29.3205 46.1903 29.431 46.0853L48.1241 27.3818L51.0517 30.3105Z"
          fill="#3B97D3"
        />
        <path
          d="M53.9792 33.2265L35.2758 51.931C35.1708 52.0415 35.0526 52.1388 34.924 52.2206L32.0378 49.3344L51.0516 30.3103L53.9792 33.2265Z"
          fill="#2980BA"
        />
        <path
          d="M56.9068 30.3093L53.9792 33.2265L51.0516 30.3093L48.124 27.3817L51.0516 24.4541L56.9068 30.3093Z"
          fill="#95A5A5"
        />
        <path
          d="M59.069 28.138L56.9069 30.3104L51.0518 24.4552L53.2138 22.2931C54.4287 21.0639 56.4094 21.05 57.6414 22.2621L59.1 23.7207C59.682 24.3104 60.0058 25.1073 60 25.9358C59.9941 26.7643 59.6592 27.5565 59.069 28.138Z"
          fill="#FF5364"
        />
        <path
          d="M30.5173 53.7931C30.519 53.8347 30.5155 53.8764 30.5069 53.9172L28.2517 54.7551C27.7838 54.9179 27.264 54.7987 26.9136 54.4484C26.5633 54.0981 26.4441 53.5782 26.6069 53.1103L27.5069 50.7C29.1847 50.7405 30.5222 52.1148 30.5173 53.7931Z"
          fill="#3F5C6C"
        />
      </svg>
    ),
  },
  {
    href: "https://matsne.gov.ge/ka/document/view/5675992?publication=0",
    titleGE: "სახელმწიფოს ახალგაზრდული<br>სტრატეგია 2023-2026",
    titleEN: "Youth of the state<br>Strategy 2023-2026",
    svg: (
      <svg
        width="60"
        height="60"
        viewBox="0 0 60 60"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M26.1917 60.0001C40.657 60.0001 52.3834 48.2737 52.3834 33.8084C52.3834 19.3431 40.657 7.6167 26.1917 7.6167C11.7264 7.6167 0 19.3431 0 33.8084C0 48.2737 11.7264 60.0001 26.1917 60.0001Z"
          fill="#FD646F"
        />
        <path
          d="M52.3835 33.8085C52.3835 48.2733 40.6566 60.0002 26.1918 60.0002C18.3428 60.0002 11.2989 56.5467 6.49951 51.0756C11.1097 55.122 17.152 57.575 23.7666 57.575C38.2315 57.575 49.9583 45.8482 49.9583 31.3833C49.9583 24.7687 47.5053 18.7264 43.4589 14.1162C48.93 18.9156 52.3835 25.9595 52.3835 33.8085Z"
          fill="#FC4755"
        />
        <path
          d="M26.1918 53.5735C37.1077 53.5735 45.9568 44.7244 45.9568 33.8085C45.9568 22.8926 37.1077 14.0435 26.1918 14.0435C15.2759 14.0435 6.42676 22.8926 6.42676 33.8085C6.42676 44.7244 15.2759 53.5735 26.1918 53.5735Z"
          fill="#F5F5F5"
        />
        <path
          d="M45.9568 33.8085C45.9568 44.7241 37.1074 53.5735 26.1918 53.5735C20.1168 53.5735 14.682 50.8319 11.0552 46.5199C14.4916 49.4083 18.926 51.1484 23.7666 51.1484C34.6823 51.1484 43.5317 42.299 43.5317 31.3833C43.5317 26.5427 41.7916 22.1083 38.9032 18.6719C43.2152 22.2987 45.9568 27.7335 45.9568 33.8085Z"
          fill="#E6E6E6"
        />
        <path
          d="M26.1916 47.1467C33.5582 47.1467 39.53 41.1749 39.53 33.8083C39.53 26.4418 33.5582 20.47 26.1916 20.47C18.8251 20.47 12.8533 26.4418 12.8533 33.8083C12.8533 41.1749 18.8251 47.1467 26.1916 47.1467Z"
          fill="#FD646F"
        />
        <path
          d="M39.53 33.8083C39.53 41.1748 33.5581 47.1467 26.1917 47.1467C21.8906 47.1467 18.0637 45.1108 15.6252 41.9496C17.877 43.6884 20.7011 44.7216 23.7665 44.7216C31.1329 44.7216 37.1049 38.7496 37.1049 31.3832C37.1049 28.3178 36.0717 25.4937 34.3329 23.2419C37.4941 25.6804 39.53 29.5073 39.53 33.8083Z"
          fill="#FC4755"
        />
        <path
          d="M26.1916 40.5989C29.9418 40.5989 32.982 37.5588 32.982 33.8085C32.982 30.0583 29.9418 27.0181 26.1916 27.0181C22.4413 27.0181 19.4011 30.0583 19.4011 33.8085C19.4011 37.5588 22.4413 40.5989 26.1916 40.5989Z"
          fill="#F5F5F5"
        />
        <path
          d="M50.9732 42.3073C50.4567 43.8134 49.808 45.2563 49.0392 46.6229L43.1921 43.8958C43.9937 42.5499 44.6387 41.0996 45.1032 39.5706L50.9732 42.3073Z"
          fill="#FC4755"
        />
        <path
          d="M45.1031 39.5706C44.6387 41.0997 43.9936 42.5499 43.1921 43.8959L37.322 41.1591C38.1878 39.8532 38.828 38.3872 39.1894 36.8132L45.1031 39.5706Z"
          fill="#E6E6E6"
        />
        <path
          d="M39.1893 36.8131C38.8279 38.387 38.1877 39.8531 37.3219 41.159L31.2566 38.3313C32.3067 37.1551 32.9542 35.6115 32.9809 33.9175L39.1893 36.8131Z"
          fill="#FC4755"
        />
        <path
          d="M32.9809 33.9176C32.9542 35.6116 32.3067 37.1552 31.2566 38.3314L25.5757 35.6819C24.465 35.1654 23.946 33.8946 24.3425 32.762C24.3692 32.6881 24.3983 32.6141 24.4322 32.5401C24.7087 31.9484 25.1974 31.5252 25.7673 31.3179C26.3348 31.1117 26.9835 31.1202 27.574 31.3967L32.9809 33.9176Z"
          fill="#E6E6E6"
        />
        <path
          d="M59.8017 10.9344C59.996 10.9668 60.0726 11.2051 59.9337 11.3448L57.3682 13.922L55.7943 15.5032L51.4935 19.8226C51.2172 20.1002 50.8237 20.2271 50.4371 20.1635L44.0225 19.107L48.8267 14.3415L50.4188 12.7628L53.3472 9.85742L59.8017 10.9344Z"
          fill="#50758D"
        />
        <path
          d="M57.3683 13.9219L55.7944 15.5031L48.8269 14.3415L50.419 12.7627L57.3683 13.9219Z"
          fill="#2B597F"
        />
        <path
          d="M50.2772 7.05763L47.3633 9.9084L45.7785 11.4593L40.9912 16.1435L39.9221 9.64672C39.8587 9.26137 39.9848 8.8691 40.2608 8.59287L44.5793 4.26991L46.1593 2.6875L48.7733 0.0713594C48.9134 -0.0688146 49.1532 0.00903291 49.1844 0.204622L50.2772 7.05763Z"
          fill="#50758D"
        />
        <path
          d="M47.3634 9.9084L45.7786 11.4593L44.5793 4.26991L46.1593 2.6875L47.3634 9.9084Z"
          fill="#2B597F"
        />
        <path
          d="M55.8199 6.66592C55.8199 7.26978 55.5895 7.87607 55.1276 8.33685L28.2459 35.2185C27.3801 36.0855 26.0087 36.1388 25.0799 35.3785C25.0192 35.3288 24.9598 35.2755 24.9028 35.2185C24.4408 34.7565 24.2104 34.1526 24.2104 33.5463C24.2104 32.9425 24.4408 32.3362 24.9028 31.8754L51.7845 4.99377C52.7072 4.06979 54.2048 4.06979 55.1276 4.99377C55.1845 5.05076 55.2379 5.11018 55.2876 5.17081C55.6429 5.60491 55.8199 6.13481 55.8199 6.66592Z"
          fill="#918291"
        />
        <path
          d="M55.8199 6.66601C55.8199 7.26987 55.5895 7.87616 55.1275 8.33694L28.2459 35.2186C27.3801 36.0856 26.0087 36.1389 25.0798 35.3786L55.2876 5.1709C55.6429 5.605 55.8199 6.1349 55.8199 6.66601Z"
          fill="#7A6E79"
        />
      </svg>
    ),
  },
];

const Legislation = ({ language }) => {
  const [hovered, setHovered] = React.useState(null);
  return (
    <div
      style={{
        display: "flex",
        maxWidth: "1350px",
        margin: "0 auto",
        gap: "40px",
        flexWrap: "wrap",
        justifyContent: "center",
        padding: "10px 0 20px",
      }}
    >
      {legislationData.map((item, i) => (
        <a
          key={i}
          href={item.href}
          target="_blank"
          rel="noreferrer"
          onMouseEnter={() => setHovered(i)}
          onMouseLeave={() => setHovered(null)}
          style={{
            display: "flex",
            flexDirection: "column",
            width: "300px",
            minHeight: "220px",
            textDecoration: "none",
            transition: "transform 0.3s",
            transform: hovered === i ? "scale(1.05)" : "scale(1)",
            border:
              hovered === i ? "1px solid #0066e0" : "1px solid transparent",
            borderRadius: "8px",
            padding: "16px",
          }}
        >
          {item.svg}
          <h5
            dangerouslySetInnerHTML={{
              __html: language === "EN" ? item.titleEN : item.titleGE,
            }}
            style={{
              color: "#37496d",
              fontFamily: "FiraGO",
              fontSize: "14px",
              marginTop: "10px",
              fontWeight: "normal",
            }}
          />
          <div
            style={{
              color: "#0066e0",
              fontFamily: "FiraGO",
              fontSize: "12px",
              fontWeight: 500,
              margin: "10px 0",
              display: "flex",
              alignItems: "center",
              gap: "5px",
              opacity: hovered === i ? 1 : 0,
              transition: "opacity 0.3s",
            }}
          >
            <span>{language === "EN" ? "View" : "ნახვა"}</span>
            <svg
              width="31"
              height="15"
              viewBox="0 0 31 15"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path d="M0 7.5H30" stroke="#0066E0" strokeMiterlimit="10" />
              <path
                d="M23.1169 1L29.8191 7.5L23.1169 14"
                stroke="#0066E0"
                strokeMiterlimit="10"
              />
            </svg>
          </div>
        </a>
      ))}
    </div>
  );
};

const linksData = [
  {
    href: "https://myrights.gov.ge/ka/achievements/1851-shshm-pirta-kanonit-gatvalistsinebuli",
    img: linkAdmin,
    titleGE: "მთავრობის<br>ადმინისტრაცია",
    titleEN: "Government of Georgia  ",
  },
  {
    href: "https://youthagency.gov.ge/",
    img: linkSport2,
    width: "93px",
    titleGE: "ახალგაზრდობის<br>სააგენტო",
    titleEN: "Youth Agency",
  },
  {
    href: "https://ombudsman.ge/geo/bavshvis-uflebebi",
    img: linkAdmin,
    titleGE: "საქართველოს სახალხო<br>დამცველი",
    titleEN: "Public Defender (Ombudsman) of Georgia",
  },
  {
    href: "https://www.unicef.org/georgia/ka",
    img: linkUnicef,
    titleGE: "გაეროს ბავშვთა ფონდი<br>(UNICEF)",
    titleEN: "UNICEF",
  },
];

const Links = ({ language }) => {
  const [hovered, setHovered] = React.useState(null);
  return (
    <div
      style={{
        display: "flex",
        maxWidth: "1350px",
        margin: "0 auto",
        gap: "40px",
        flexWrap: "wrap",
        justifyContent: "center",
        padding: "10px 0 20px",
      }}
    >
      {linksData.map((item, i) => (
        <a
          key={i}
          href={item.href}
          target="_blank"
          rel="noreferrer"
          onMouseEnter={() => setHovered(i)}
          onMouseLeave={() => setHovered(null)}
          style={{
            display: "flex",
            flexDirection: "column",
            justifyContent: "center",
            alignItems: "center",
            width: "300px",
            height: "220px",
            borderRadius: "15px",
            border: "0.5px solid transparent",
            textDecoration: "none",
            background:
              hovered === i
                ? "linear-gradient(90deg, rgba(224,234,252,0.8) 0%, rgba(207,222,243,0.8) 100%)"
                : "linear-gradient(90deg, rgba(224,234,252,0.35) 0%, rgba(207,222,243,0.35) 100%)",
            transform: hovered === i ? "scale(1.05)" : "scale(1)",
            transition: "all 0.3s",
          }}
        >
          <img
            src={item.img}
            alt={item.titleGE}
            style={{
              width: item.width || "auto",
              maxHeight: "80px",
              objectFit: "contain",
            }}
          />
          <h5
            style={{
              color: "#37496d",
              textAlign: "center",
              fontFeatureSettings: '"case" on',
              fontFamily: "FiraGO",
              fontSize: "16px",
              marginTop: "10px",
              fontWeight: "bold",
              padding: "0 12px",
            }}
          >
            <span
              dangerouslySetInnerHTML={{
                __html: language === "EN" ? item.titleEN : item.titleGE,
              }}
            />
          </h5>
        </a>
      ))}
    </div>
  );
};

const statCardsData = [
  {
    titleGE: "ძირითადი მაჩვენებელი",
    titleEN: "Environment",
    route: "/statistics/main-indicators",
    svg: (
      <svg
        width="60"
        height="60"
        viewBox="0 0 60 60"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M8.67188 60C3.89016 60 0 56.1098 0 51.3281V29.0625C0 24.2808 3.89016 20.3906 8.67188 20.3906C13.4536 20.3906 17.3438 24.2808 17.3438 29.0625V51.3281C17.3438 56.1098 13.4536 60 8.67188 60ZM8.67188 25.0781C6.47484 25.0781 4.6875 26.8655 4.6875 29.0625V51.3281C4.6875 53.5252 6.47484 55.3125 8.67188 55.3125C10.8689 55.3125 12.6562 53.5252 12.6562 51.3281V29.0625C12.6562 26.8656 10.8689 25.0781 8.67188 25.0781ZM30 60C25.2183 60 21.3281 56.1098 21.3281 51.3281V41.7188C21.3281 36.937 25.2183 33.0469 30 33.0469C34.7817 33.0469 38.6719 36.937 38.6719 41.7188V51.3281C38.6719 56.1098 34.7817 60 30 60ZM30 37.7344C27.803 37.7344 26.0156 39.5217 26.0156 41.7188V51.3281C26.0156 53.5252 27.803 55.3125 30 55.3125C32.197 55.3125 33.9844 53.5252 33.9844 51.3281V41.7188C33.9844 39.5218 32.197 37.7344 30 37.7344ZM51.3281 60C46.5464 60 42.6562 56.1098 42.6562 51.3281V30.8203C42.6562 26.0386 46.5464 22.1484 51.3281 22.1484C56.1098 22.1484 60 26.0386 60 30.8203V51.3281C60 56.1098 56.1098 60 51.3281 60ZM51.3281 26.8359C49.1311 26.8359 47.3438 28.6233 47.3438 30.8203V51.3281C47.3438 53.5252 49.1311 55.3125 51.3281 55.3125C53.5252 55.3125 55.3125 53.5252 55.3125 51.3281V30.8203C55.3125 28.6234 53.5252 26.8359 51.3281 26.8359ZM38.2975 25.9043L51.2679 12.2801C52.1604 11.3426 52.1239 9.85898 51.1864 8.96648C50.2489 8.07387 48.7655 8.11043 47.873 9.04793L34.8814 22.6942C34.8663 22.71 34.8514 22.7261 34.8368 22.7423C33.6151 24.1 31.5469 24.2907 30.0981 23.1908L15.2177 9.67313C15.1781 9.63727 15.1375 9.6027 15.0956 9.56953C11.9502 7.08211 7.50328 7.22297 4.52168 9.90445C4.49414 9.9293 4.46707 9.95473 4.44082 9.98074L0.695625 13.6853C-0.224766 14.5956 -0.232852 16.0795 0.677578 16.9998C1.58766 17.92 3.0716 17.9282 3.99199 17.0179L7.69254 13.3575C8.95254 12.2549 10.8009 12.1918 12.1325 13.2034L27.0131 26.7212C27.0526 26.7571 27.0934 26.7916 27.1352 26.8247C28.6325 28.0089 30.4287 28.588 32.2164 28.5879C34.4593 28.5878 36.6889 27.6761 38.2975 25.9043ZM60 13.3594V7.03125C60 3.15422 56.8458 0 52.9688 0H46.6406C45.3462 0 44.2969 1.0493 44.2969 2.34375C44.2969 3.6382 45.3462 4.6875 46.6406 4.6875H52.9688C54.2611 4.6875 55.3125 5.73891 55.3125 7.03125V13.3594C55.3125 14.6538 56.3618 15.7031 57.6562 15.7031C58.9507 15.7031 60 14.6538 60 13.3594Z"
          fill="url(#paint0_linear_1_5)"
        />
        <defs>
          <linearGradient
            id="paint0_linear_1_5"
            x1="0"
            y1="30"
            x2="60"
            y2="30"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#DF260D" />
            <stop offset="1" stopColor="#D37632" />
          </linearGradient>
        </defs>
      </svg>
    ),
  },
  {
    titleGE: "მოსახლეობა",
    titleEN: "Population",
    route: "/statistics/population",
    svg: (
      <svg
        width="80"
        height="59"
        viewBox="0 0 80 59"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M39.6454 26.3426C42.2505 26.3426 44.7972 25.57 46.9632 24.1226C49.1293 22.6752 50.8175 20.618 51.8143 18.2111C52.8112 15.8042 53.0718 13.1558 52.5634 10.6007C52.055 8.04566 50.8003 5.69877 48.958 3.85684C47.1157 2.01491 44.7685 0.760679 42.2134 0.252764C39.6582 -0.255152 37.0098 0.0060628 34.6031 1.00337C32.1964 2.00069 30.1395 3.6893 28.6926 5.85565C27.2456 8.02201 26.4736 10.5688 26.4741 13.1739C26.4784 16.6656 27.8675 20.0131 30.3367 22.4818C32.806 24.9506 36.1537 26.339 39.6454 26.3426ZM39.6454 5.33409C41.196 5.33409 42.7117 5.79389 44.001 6.65535C45.2902 7.5168 46.2951 8.74122 46.8885 10.1738C47.4819 11.6063 47.6371 13.1826 47.3346 14.7034C47.0321 16.2242 46.2854 17.6211 45.189 18.7176C44.0926 19.814 42.6957 20.5607 41.1749 20.8632C39.6541 21.1657 38.0778 21.0104 36.6452 20.417C35.2127 19.8237 33.9883 18.8188 33.1268 17.5295C32.2653 16.2403 31.8055 14.7245 31.8055 13.1739C31.8084 11.0956 32.6353 9.1031 34.1049 7.63345C35.5745 6.1638 37.567 5.33691 39.6454 5.33409Z"
          fill="url(#paint0_linear_586_4372)"
        />
        <path
          d="M14.7635 34.0838C16.767 34.0838 18.7254 33.4897 20.3913 32.3766C22.0571 31.2636 23.3554 29.6815 24.1221 27.8305C24.8888 25.9796 25.0894 23.9428 24.6986 21.9779C24.3077 20.0129 23.3429 18.208 21.9263 16.7913C20.5096 15.3746 18.7047 14.4099 16.7397 14.019C14.7747 13.6282 12.738 13.8288 10.887 14.5955C9.03606 15.3621 7.45402 16.6605 6.34095 18.3263C5.22789 19.9921 4.63379 21.9506 4.63379 23.9541C4.63379 26.6406 5.70102 29.2172 7.60071 31.1169C9.5004 33.0165 12.0769 34.0838 14.7635 34.0838ZM14.7635 19.1558C15.7125 19.1558 16.6402 19.4372 17.4293 19.9645C18.2183 20.4917 18.8334 21.2411 19.1965 22.1179C19.5597 22.9946 19.6547 23.9594 19.4696 24.8902C19.2844 25.821 18.8274 26.6759 18.1564 27.347C17.4853 28.018 16.6304 28.475 15.6996 28.6602C14.7688 28.8453 13.804 28.7503 12.9273 28.3871C12.0505 28.0239 11.3011 27.4089 10.7739 26.6199C10.2466 25.8308 9.96521 24.9031 9.96521 23.9541C9.96521 22.6815 10.4707 21.461 11.3706 20.5612C12.2704 19.6613 13.4909 19.1558 14.7635 19.1558Z"
          fill="url(#paint1_linear_586_4372)"
        />
        <path
          d="M64.5247 34.0838C66.5282 34.0838 68.4867 33.4897 70.1525 32.3766C71.8183 31.2636 73.1167 29.6815 73.8833 27.8305C74.65 25.9796 74.8506 23.9428 74.4598 21.9779C74.0689 20.0129 73.1042 18.208 71.6875 16.7913C70.2708 15.3746 68.4659 14.4099 66.5009 14.019C64.536 13.6282 62.4992 13.8288 60.6483 14.5955C58.7973 15.3621 57.2152 16.6605 56.1022 18.3263C54.9891 19.9921 54.395 21.9506 54.395 23.9541C54.395 26.6406 55.4623 29.2172 57.3619 31.1169C59.2616 33.0165 61.8382 34.0838 64.5247 34.0838ZM64.5247 19.1558C65.4737 19.1558 66.4014 19.4372 67.1905 19.9645C67.9796 20.4917 68.5946 21.2411 68.9578 22.1179C69.3209 22.9946 69.4159 23.9594 69.2308 24.8902C69.0457 25.821 68.5887 26.6759 67.9176 27.347C67.2466 28.018 66.3916 28.475 65.4608 28.6602C64.53 28.8453 63.5653 28.7503 62.6885 28.3871C61.8117 28.0239 61.0623 27.4089 60.5351 26.6199C60.0079 25.8308 59.7264 24.9031 59.7264 23.9541C59.7264 22.6815 60.232 21.461 61.1318 20.5612C62.0317 19.6613 63.2521 19.1558 64.5247 19.1558Z"
          fill="url(#paint2_linear_586_4372)"
        />
        <path
          d="M64.8791 39.4312C62.5422 39.4467 60.2461 40.045 58.1988 41.1719C56.2525 38.0906 53.5698 35.5422 50.3927 33.7566C47.2156 31.971 43.644 31.0042 40 30.9436C36.356 31.0042 32.7844 31.971 29.6073 33.7566C26.4302 35.5422 23.7475 38.0906 21.8012 41.1719C19.7539 40.045 17.4578 39.4467 15.1209 39.4312C10.9275 39.6358 6.98529 41.4912 4.15499 44.5922C1.32468 47.6931 -0.163855 51.7879 0.0143382 55.9826C0.0143382 56.6896 0.295189 57.3676 0.795107 57.8675C1.29502 58.3675 1.97306 58.6483 2.68005 58.6483C3.38704 58.6483 4.06507 58.3675 4.56499 57.8675C5.06491 57.3676 5.34576 56.6896 5.34576 55.9826C5.17104 53.2024 6.09925 50.4654 7.92913 48.3651C9.759 46.2648 12.343 44.9704 15.1209 44.7626C16.594 44.7739 18.0403 45.1582 19.3247 45.8795C18.0684 49.0457 17.4252 52.4217 17.4294 55.828C17.4294 56.535 17.7103 57.213 18.2102 57.7129C18.7101 58.2128 19.3881 58.4937 20.0951 58.4937C20.8021 58.4937 21.4802 58.2128 21.9801 57.7129C22.48 57.213 22.7608 56.535 22.7608 55.828C22.7608 45.0452 30.4914 36.275 40 36.275C49.5086 36.275 57.2391 45.0452 57.2391 55.828C57.2391 56.535 57.52 57.213 58.0199 57.7129C58.5198 58.2128 59.1979 58.4937 59.9049 58.4937C60.6118 58.4937 61.2899 58.2128 61.7898 57.7129C62.2897 57.213 62.5706 56.535 62.5706 55.828C62.5748 52.4217 61.9316 49.0457 60.6753 45.8795C61.9597 45.1582 63.406 44.7739 64.8791 44.7626C67.657 44.9704 70.241 46.2648 72.0709 48.3651C73.9007 50.4654 74.829 53.2024 74.6542 55.9826C74.6542 56.6896 74.9351 57.3676 75.435 57.8675C75.9349 58.3675 76.613 58.6483 77.3199 58.6483C78.0269 58.6483 78.705 58.3675 79.2049 57.8675C79.7048 57.3676 79.9857 56.6896 79.9857 55.9826C80.1638 51.7879 78.6753 47.6931 75.845 44.5922C73.0147 41.4912 69.0725 39.6358 64.8791 39.4312Z"
          fill="url(#paint3_linear_586_4372)"
        />
        <defs>
          <linearGradient
            id="paint0_linear_586_4372"
            x1="26.4741"
            y1="13.1713"
            x2="52.8167"
            y2="13.1713"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#FF512F" />
            <stop offset="1" stopColor="#F09819" />
          </linearGradient>
          <linearGradient
            id="paint1_linear_586_4372"
            x1="4.63379"
            y1="23.9541"
            x2="24.8932"
            y2="23.9541"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#FF512F" />
            <stop offset="1" stopColor="#F09819" />
          </linearGradient>
          <linearGradient
            id="paint2_linear_586_4372"
            x1="54.395"
            y1="23.9541"
            x2="74.6544"
            y2="23.9541"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#FF512F" />
            <stop offset="1" stopColor="#F09819" />
          </linearGradient>
          <linearGradient
            id="paint3_linear_586_4372"
            x1="0"
            y1="44.7959"
            x2="80"
            y2="44.7959"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#FF512F" />
            <stop offset="1" stopColor="#F09819" />
          </linearGradient>
        </defs>
      </svg>
    ),
  },
  {
    titleGE: "ჯანმრთელობის დაცვა",
    titleEN: "Health Care",
    route: "/statistics/health-care",
    svg: (
      <svg
        width="80"
        height="66"
        viewBox="0 0 80 66"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M77.6562 30.3369H74.5034C75.9523 27.457 76.7188 24.2573 76.7188 20.9397C76.7188 15.3466 74.5406 10.0881 70.5858 6.13312C66.6308 2.17812 61.3723 0 55.7792 0C50.1861 0 44.9277 2.17812 40.9727 6.13312L40 7.10578L39.0273 6.13328C35.0723 2.17828 29.8141 0.000156164 24.2208 0.000156164C18.6275 0.000156164 13.3692 2.17828 9.41422 6.13312C5.45937 10.0881 3.28125 15.3466 3.28125 20.9397C3.28125 24.2572 4.04766 27.457 5.49656 30.3369H2.34375C1.04937 30.3369 0 31.3863 0 32.6806C0 33.975 1.04937 35.0244 2.34375 35.0244H8.72594C8.94922 35.2694 9.17844 35.5103 9.41438 35.7463L38.3428 64.6747C38.8005 65.1323 39.4003 65.3613 40 65.3613C40.5997 65.3613 41.1997 65.1325 41.6572 64.6747L70.5856 35.7463C70.8216 35.5103 71.0508 35.2694 71.2741 35.0244H77.6562C78.9506 35.0244 80 33.975 80 32.6806C80 31.3863 78.9506 30.3369 77.6562 30.3369ZM7.96875 20.9397C7.96875 16.5986 9.65922 12.5173 12.7289 9.44766C15.7984 6.37797 19.8797 4.6875 24.2208 4.6875C28.5619 4.6875 32.6431 6.37797 35.7128 9.44766L38.3427 12.0775C39.2578 12.9928 40.7419 12.9928 41.6572 12.0775L44.287 9.44766C47.3567 6.37797 51.438 4.6875 55.7791 4.6875C60.1202 4.6875 64.2014 6.37797 67.2711 9.44766C70.3408 12.5173 72.0312 16.5986 72.0312 20.9397C72.0312 24.355 70.9842 27.6092 69.0431 30.3369H56.7586L51.5112 22.0516C51.0716 21.3573 50.3012 20.9452 49.4791 20.9625C48.6577 20.9808 47.9058 21.4278 47.4973 22.1409L39.8138 35.5567L29.9031 15.9964C29.4903 15.1816 28.6384 14.6795 27.7289 14.7134C26.8161 14.7459 26.0053 15.3059 25.6516 16.1481L19.6923 30.3369H10.9569C9.01578 27.6092 7.96875 24.3552 7.96875 20.9397ZM40 59.7028L15.3216 35.0244H21.25C22.1938 35.0244 23.0455 34.4583 23.4109 33.5883L28.0116 22.6344L37.5969 41.5525C37.9862 42.3213 38.7661 42.8142 39.6277 42.8363C39.6478 42.8367 39.6678 42.837 39.6878 42.837C40.5262 42.837 41.3031 42.3883 41.7214 41.6581L49.6322 27.8456L53.4888 33.935C53.9184 34.6131 54.6656 35.0244 55.4688 35.0244H64.6784L40 59.7028Z"
          fill="url(#paint0_linear_17_121)"
        />
        <defs>
          <linearGradient
            id="paint0_linear_17_121"
            x1="0"
            y1="32.6806"
            x2="80"
            y2="32.6806"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#FF512F" />
            <stop offset="1" stopColor="#DD2476" />
          </linearGradient>
        </defs>
      </svg>
    ),
  },
  {
    titleGE: "განათლება",
    titleEN: "Education",
    route: "/statistics/education",
    svg: (
      <svg
        width="80"
        height="66"
        viewBox="0 0 80 66"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M77.1545 51.2757V28.5526L77.8029 28.175C79.1792 27.3761 80 25.9506 80 24.361C80 22.7713 79.1792 21.3431 77.8029 20.5441L44.5405 1.222C41.7415 -0.4087 38.2585 -0.405964 35.4595 1.222L2.19433 20.5469C0.820821 21.3458 0 22.7713 0 24.361C0 25.9506 0.820821 27.3761 2.19433 28.1778L12.2795 34.0384V47.6613C12.2795 50.8242 13.9813 53.7655 16.7174 55.3442L30.6002 63.3472C33.4977 65.0162 36.7482 65.8535 39.9986 65.8535C43.2491 65.8535 46.4968 65.019 49.397 63.3472L63.2771 55.3442C66.0159 53.7683 67.7178 50.8242 67.7178 47.6613V34.0357L72.2296 31.4145V51.2757C71.0722 52.0637 70.3116 53.3907 70.3116 54.8983C70.3116 57.3169 72.2733 59.2787 74.692 59.2787C77.1107 59.2787 79.0725 57.3169 79.0725 54.8983C79.0725 53.3934 78.3118 52.0664 77.1545 51.2757ZM62.7956 47.6586H62.7928C62.7928 49.0649 62.0404 50.3728 60.8201 51.0732L46.94 59.0762C42.6608 61.5414 37.3392 61.5414 33.0627 59.0762L19.1799 51.0732C17.9623 50.3728 17.2072 49.0649 17.2072 47.6586V36.8976L35.4595 47.5026C36.8604 48.3153 38.4309 48.7229 40.0014 48.7229C41.5719 48.7229 43.1424 48.318 44.5432 47.5026L62.7956 36.8976V47.6586ZM42.0671 43.2453C40.7921 43.9868 39.2079 43.9868 37.9329 43.2453L5.4311 24.361L37.9302 5.47659C39.2052 4.73511 40.7894 4.73511 42.0644 5.47659L74.5662 24.3582L42.0671 43.2453Z"
          fill="url(#paint0_linear_17_124)"
        />
        <defs>
          <linearGradient
            id="paint0_linear_17_124"
            x1="0"
            y1="32.9267"
            x2="80"
            y2="32.9267"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#642B73" />
            <stop offset="1" stopColor="#C6426E" />
          </linearGradient>
        </defs>
      </svg>
    ),
  },
  {
    titleGE: "სოციალური უზრუნველყოფა",
    titleEN: "Social Protection",
    route: "/statistics/social-protection",
    svg: (
      <svg
        width="80"
        height="91"
        viewBox="0 0 80 91"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M1.61621 16.2156L38.9495 0.215613C39.6203 -0.071871 40.3797 -0.071871 41.0505 0.215613L78.3838 16.2156C79.3643 16.6358 80 17.5999 80 18.6667V37.0816C80 60.748 65.1224 81.8593 42.8348 89.8192L40.8969 90.5113C40.3169 90.7184 39.6831 90.7184 39.1031 90.5113L37.1652 89.8192C14.8776 81.8593 0 60.748 0 37.0816V18.6667C0 17.5999 0.635722 16.6358 1.61621 16.2156ZM5.33333 20.425V37.0816C5.33333 58.494 18.794 77.5948 38.959 84.7965L40 85.1684L41.041 84.7965C61.206 77.5948 74.6667 58.494 74.6667 37.0816V20.425L40 5.56791L5.33333 20.425ZM54.6667 58.6667C54.6667 60.1394 53.4728 61.3333 52 61.3333C50.5272 61.3333 49.3333 60.1394 49.3333 58.6667V56.0052C49.3333 52.874 47.2576 50.6667 44.6667 50.6667H36.0045C32.8065 50.6667 30.6667 52.8057 30.6667 56V58.6667C30.6667 60.1394 29.4728 61.3333 28 61.3333C26.5272 61.3333 25.3333 60.1394 25.3333 58.6667V56C25.3333 49.8597 29.8615 45.3333 36.0045 45.3333H44.6667C50.279 45.3333 54.6667 49.9994 54.6667 56.0052V58.6667ZM40 41.3333C34.8453 41.3333 30.6667 37.1547 30.6667 32C30.6667 26.8453 34.8453 22.6667 40 22.6667C45.1547 22.6667 49.3333 26.8453 49.3333 32C49.3333 37.1547 45.1547 41.3333 40 41.3333ZM40 36C42.2091 36 44 34.2091 44 32C44 29.7909 42.2091 28 40 28C37.7909 28 36 29.7909 36 32C36 34.2091 37.7909 36 40 36Z"
          fill="url(#paint0_linear_31_307)"
        />
        <defs>
          <linearGradient
            id="paint0_linear_31_307"
            x1="0"
            y1="45.3333"
            x2="80"
            y2="45.3333"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#5F2C82" />
            <stop offset="1" stopColor="#49A09D" />
          </linearGradient>
        </defs>
      </svg>
    ),
  },
  {
    titleGE: "ცხოვრების დონე",
    titleEN: "Living Standards",
    route: "/statistics/living-standards",
    svg: (
      <svg
        width="80"
        height="80"
        viewBox="0 0 80 80"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M54.2188 47.1875C55.5111 47.1875 56.5625 48.2248 56.5625 49.5H61.25C61.25 46.4581 59.2905 43.8645 56.5625 42.9014V37.8125H51.875V42.9031C49.147 43.8706 47.1875 46.4758 47.1875 49.5312C47.1875 53.4083 50.3417 56.5625 54.2188 56.5625C55.5111 56.5625 56.5625 57.6139 56.5625 58.9062C56.5625 60.1986 55.5111 61.25 54.2188 61.25C52.9264 61.25 51.875 60.1986 51.875 58.9062H47.1875C47.1875 61.9617 49.147 64.5669 51.875 65.5344V70.5937H56.5625V65.5344C59.2905 64.5669 61.25 61.9617 61.25 58.9062C61.25 55.0292 58.0958 51.875 54.2188 51.875C52.9264 51.875 51.875 50.8236 51.875 49.5312C51.875 48.2389 52.9264 47.1875 54.2188 47.1875Z"
          fill="url(#paint0_linear_31_308)"
        />
        <path
          d="M54.2188 28.4375C53.4286 28.4375 52.6472 28.4753 51.875 28.5452V14.0625C51.875 9.99828 48.9769 6.31094 43.7147 3.67984C38.9687 1.30687 32.7109 0 26.0938 0C19.4631 0 13.1552 1.30422 8.33172 3.67219C2.95906 6.31016 0 10 0 14.0625V56.5625C0 60.625 2.95906 64.315 8.33172 66.9528C13.1552 69.3208 19.4631 70.625 26.0938 70.625C28.7514 70.625 31.3511 70.4128 33.8461 69.9994C38.5666 76.0795 45.943 80 54.2188 80C68.4345 80 80 68.4345 80 54.2188C80 40.003 68.4345 28.4375 54.2188 28.4375ZM10.3977 7.88C14.53 5.85109 20.2509 4.6875 26.0938 4.6875C38.5247 4.6875 47.1875 9.62828 47.1875 14.0625C47.1875 18.4967 38.5247 23.4375 26.0938 23.4375C20.2509 23.4375 14.53 22.2739 10.3977 20.245C6.76875 18.4634 4.6875 16.21 4.6875 14.0625C4.6875 11.915 6.76875 9.66156 10.3977 7.88ZM4.6875 22.2289C5.73812 23.0316 6.955 23.7769 8.33172 24.4528C13.1552 26.8208 19.4631 28.125 26.0938 28.125C32.7109 28.125 38.9687 26.8181 43.7147 24.4452C45.0208 23.792 46.1808 23.0739 47.1875 22.3011V29.412C42.5163 30.7378 38.3814 33.3509 35.1909 36.8414C32.3353 37.4856 29.2791 37.8125 26.0938 37.8125C20.2509 37.8125 14.53 36.6489 10.3977 34.62C6.76875 32.8384 4.6875 30.585 4.6875 28.4375V22.2289ZM4.6875 36.6039C5.73812 37.4066 6.955 38.1519 8.33172 38.8278C13.1552 41.1958 19.4631 42.5 26.0938 42.5C27.9033 42.5 29.6784 42.403 31.4095 42.2127C29.8695 45.1267 28.8717 48.3691 28.5513 51.8056C27.737 51.8503 26.9161 51.875 26.0938 51.875C20.2509 51.875 14.53 50.7114 10.3977 48.6825C6.76875 46.9009 4.6875 44.6475 4.6875 42.5V36.6039ZM26.0938 65.9375C20.2509 65.9375 14.53 64.7739 10.3977 62.745C6.76875 60.9634 4.6875 58.71 4.6875 56.5625V50.6663C5.73812 51.4689 6.955 52.2142 8.33172 52.8902C13.1552 55.2583 19.4631 56.5625 26.0938 56.5625C26.9109 56.5625 27.728 56.5419 28.5397 56.5019C28.8272 59.7625 29.7239 62.8503 31.1156 65.6514C29.4813 65.84 27.8027 65.9375 26.0938 65.9375ZM54.2188 75.3125C42.5877 75.3125 33.125 65.8498 33.125 54.2188C33.125 42.5877 42.5877 33.125 54.2188 33.125C65.8498 33.125 75.3125 42.5877 75.3125 54.2188C75.3125 65.8498 65.8498 75.3125 54.2188 75.3125Z"
          fill="url(#paint1_linear_31_308)"
        />
        <defs>
          <linearGradient
            id="paint0_linear_31_308"
            x1="47.1875"
            y1="54.2031"
            x2="61.25"
            y2="54.2031"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#005C97" />
            <stop offset="1" stopColor="#363795" />
          </linearGradient>
          <linearGradient
            id="paint1_linear_31_308"
            x1="0"
            y1="40"
            x2="80"
            y2="40"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#005C97" />
            <stop offset="1" stopColor="#363795" />
          </linearGradient>
        </defs>
      </svg>
    ),
  },
  {
    titleGE: "დასაქმება და უმუშევრობა",
    titleEN: "Employment and Unemployment",
    route: "/statistics/employment",
    svg: (
      <svg
        width="80"
        height="71"
        viewBox="0 0 80 71"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M77.6709 9.37561C77.666 9.37561 77.6611 9.375 77.6562 9.375H56.4062V7.03125C56.4062 3.1543 53.252 0 49.375 0H30.625C26.748 0 23.5938 3.1543 23.5938 7.03125V9.375H2.34375C1.03882 9.375 0 10.4443 0 11.7188V63.2812C0 67.1582 3.1543 70.3125 7.03125 70.3125H72.9688C76.8457 70.3125 80 67.1582 80 63.2812V11.7682C80 11.7651 80 11.7621 80 11.759C79.9103 10.2197 78.9636 9.38416 77.6709 9.37561ZM28.2812 7.03125C28.2812 5.73914 29.3329 4.6875 30.625 4.6875H49.375C50.6671 4.6875 51.7188 5.73914 51.7188 7.03125V9.375H28.2812V7.03125ZM74.4043 14.0625L67.1259 35.8972C66.8066 36.8561 65.9131 37.5 64.903 37.5H51.7188V35.1562C51.7188 33.8617 50.6696 32.8125 49.375 32.8125H30.625C29.3304 32.8125 28.2812 33.8617 28.2812 35.1562V37.5H15.097C14.0869 37.5 13.1934 36.8561 12.8741 35.8972L5.5957 14.0625H74.4043ZM47.0312 37.5V42.1875H32.9688V37.5H47.0312ZM75.3125 63.2812C75.3125 64.5734 74.2609 65.625 72.9688 65.625H7.03125C5.73914 65.625 4.6875 64.5734 4.6875 63.2812V26.1615L8.42712 37.3798C9.38538 40.2557 12.066 42.1875 15.097 42.1875H28.2812V44.5312C28.2812 45.8258 29.3304 46.875 30.625 46.875H49.375C50.6696 46.875 51.7188 45.8258 51.7188 44.5312V42.1875H64.903C67.934 42.1875 70.6146 40.2557 71.5729 37.3798L75.3125 26.1615V63.2812Z"
          fill="url(#paint0_linear_31_311)"
        />
        <defs>
          <linearGradient
            id="paint0_linear_31_311"
            x1="0"
            y1="35.1562"
            x2="80"
            y2="35.1562"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#0575E6" />
            <stop offset="1" stopColor="#021B79" />
          </linearGradient>
        </defs>
      </svg>
    ),
  },
  {
    titleGE: "საინფ. და საკომ. ტექნოლოგიები",
    titleEN: "ICT",
    route: "/statistics/ict",
    svg: (
      <svg
        width="80"
        height="72"
        viewBox="0 0 80 72"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M69.9995 0H9.99973C4.47948 0.00585936 0.00585936 4.47948 0 10.0005V50.0001C0.00585936 55.5203 4.47948 59.994 9.99973 59.9998H29.7377C29.173 62.8079 27.4877 65.6841 24.5859 68.586C24.0138 69.158 23.8432 70.0179 24.1523 70.7656C24.4621 71.5127 25.1908 71.9998 26.0002 71.9998H53.9998C54.8084 71.9998 55.5379 71.5127 55.8477 70.7656C56.1568 70.0179 55.9862 69.158 55.4141 68.586C52.5123 65.6841 50.827 62.8079 50.2623 59.9998H69.9995C75.5205 59.994 79.9941 55.5203 80 50.0001V10.0005C79.9941 4.47948 75.5205 0.00585936 69.9995 0ZM9.99973 4.00048H69.9995C73.3123 4.00414 75.9959 6.68772 75.9995 10.0005V47.9999H3.99974V10.0005C4.00341 6.68772 6.68772 4.00414 9.99973 4.00048ZM49.6288 68H30.3712C32.1649 65.66 33.3493 62.9112 33.8187 59.9998H46.1813C46.6507 62.9112 47.8351 65.66 49.6288 68ZM69.9995 56.0001H9.99973C7.46555 55.992 5.21043 54.3902 4.36815 52.0003H75.6311C74.7896 54.3902 72.5344 55.992 69.9995 56.0001Z"
          fill="url(#paint0_linear_31_318)"
        />
        <path
          d="M35.3671 39.8964C35.8702 40.0648 36.4196 40.0267 36.8949 39.7894C37.3695 39.5529 37.7306 39.1361 37.8983 38.6329L45.8985 14.633C46.2435 13.5857 45.6766 12.457 44.6307 12.1084C43.5848 11.759 42.4532 12.3222 42.1017 13.3674L34.1015 37.3673C33.7528 38.4154 34.3197 39.547 35.3671 39.8964Z"
          fill="url(#paint1_linear_31_318)"
        />
        <path
          d="M26.8008 33.6005C27.6848 34.2626 28.938 34.0831 29.6001 33.1991C30.2622 32.3158 30.0827 31.0626 29.1994 30.3998L23.332 26.0002L29.1994 21.6005C30.0827 20.9377 30.2622 19.6845 29.6001 18.8012C28.938 17.9172 27.6848 17.7377 26.8008 18.3998L18.8005 24.3998C18.2966 24.7778 18 25.3703 18 25.9994C18 26.6293 18.2966 27.2218 18.8005 27.599L26.8008 33.6005Z"
          fill="url(#paint2_linear_31_318)"
        />
        <path
          d="M50.3985 33.1991C51.0613 34.0831 52.3152 34.2626 53.1993 33.599L61.1988 27.599C61.7034 27.2218 62 26.6293 62 25.9994C62 25.3703 61.7034 24.7778 61.1988 24.3998L53.1993 18.3998C52.3152 17.7377 51.0621 17.9172 50.4 18.8012C49.7379 19.6845 49.9173 20.9377 50.8006 21.6005L56.668 26.0002L50.8006 30.3998C49.918 31.0626 49.7379 32.3151 50.3985 33.1991Z"
          fill="url(#paint3_linear_31_318)"
        />
        <defs>
          <linearGradient
            id="paint0_linear_31_318"
            x1="0"
            y1="35.9999"
            x2="80"
            y2="35.9999"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#76B852" />
            <stop offset="1" stopColor="#8DC26F" />
          </linearGradient>
          <linearGradient
            id="paint1_linear_31_318"
            x1="33.9987"
            y1="26.0024"
            x2="45.9996"
            y2="26.0024"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#76B852" />
            <stop offset="1" stopColor="#8DC26F" />
          </linearGradient>
          <linearGradient
            id="paint2_linear_31_318"
            x1="18"
            y1="26.0002"
            x2="29.9996"
            y2="26.0002"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#76B852" />
            <stop offset="1" stopColor="#8DC26F" />
          </linearGradient>
          <linearGradient
            id="paint3_linear_31_318"
            x1="50.0004"
            y1="26"
            x2="62"
            y2="26"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#76B852" />
            <stop offset="1" stopColor="#8DC26F" />
          </linearGradient>
        </defs>
      </svg>
    ),
  },
  {
    titleGE: "ტურიზმი",
    titleEN: "Tourism",
    route: "/statistics/tourism",
    svg: (
      <svg
        width="80"
        height="82"
        viewBox="0 0 80 82"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M79.0267 17.7425C78.7232 17.503 78.3695 17.3353 77.992 17.2519C77.6145 17.1686 77.2231 17.1718 76.847 17.2612L68.5121 19.2474C68.9479 13.8143 67.1905 8.96074 63.4017 5.1718C60.0674 1.83656 55.5747 0 50.7523 0C45.9299 0 41.4371 1.83656 38.1022 5.1718C34.5086 8.76524 32.7414 13.3165 32.9405 18.4116L28.1136 17.2617C27.7234 17.1689 27.317 17.1689 26.9268 17.2617L1.9663 23.2094C1.40627 23.3429 0.907591 23.6612 0.550806 24.113C0.194021 24.5649 -2.99407e-05 25.1238 3.46502e-09 25.6995V79.3495C4.05767e-06 79.7361 0.0875788 80.1177 0.256151 80.4656C0.424723 80.8135 0.669912 81.1187 0.973315 81.3583C1.27672 81.5979 1.63045 81.7657 2.00796 81.849C2.38547 81.9323 2.77696 81.9291 3.15302 81.8394L27.5199 76.0322L51.8868 81.8394C52.2769 81.9324 52.6834 81.9324 53.0735 81.8394L78.0337 75.8914C78.5937 75.758 79.0924 75.4396 79.4492 74.9878C79.806 74.536 80 73.9771 80 73.4014V19.7513C80 19.3647 79.9124 18.9831 79.7439 18.6352C79.5753 18.2873 79.3301 17.9821 79.0267 17.7425ZM41.722 8.79164C44.1386 6.43781 47.3788 5.1206 50.7523 5.1206C54.1259 5.1206 57.366 6.43781 59.7827 8.79164C62.6378 11.6465 63.8402 15.2151 63.3569 19.3978C62.4399 27.3314 55.8539 35.6255 50.7525 39.4407C45.6503 35.6255 39.0651 27.3314 38.1481 19.3978C37.6643 15.2151 38.8669 11.6465 41.722 8.79164ZM74.8807 71.3802L52.4801 76.7182L28.1136 70.9114C27.7234 70.8187 27.317 70.8187 26.9268 70.9114L5.11932 76.1082V27.7207L27.5199 22.3826L33.8525 23.8917C35.1139 28.3057 37.4303 32.1662 39.3692 34.8599C40.6752 36.6722 42.1148 38.3844 43.6762 39.9821H40.7965C39.8038 39.9775 38.8199 40.1691 37.9014 40.5458C36.9829 40.9226 36.1479 41.4771 35.4442 42.1774C34.7406 42.8778 34.1823 43.7103 33.8013 44.627C33.4204 45.5438 33.2242 46.5268 33.2242 47.5196C33.2242 48.5123 33.4204 49.4953 33.8013 50.4121C34.1823 51.3288 34.7406 52.1613 35.4442 52.8617C36.1479 53.5621 36.9829 54.1166 37.9014 54.4933C38.8199 54.87 39.8038 55.0616 40.7965 55.0571H54.5227C55.465 55.0571 56.3687 55.4314 57.0351 56.0977C57.7014 56.764 58.0757 57.6677 58.0757 58.61C58.0757 59.5523 57.7014 60.456 57.0351 61.1224C56.3687 61.7887 55.465 62.163 54.5227 62.163H24.1695C23.4906 62.163 22.8395 62.4327 22.3595 62.9127C21.8795 63.3927 21.6098 64.0438 21.6098 64.7227C21.6098 65.4015 21.8795 66.0526 22.3595 66.5326C22.8395 67.0126 23.4906 67.2823 24.1695 67.2823H54.5234C56.8234 67.2823 59.0292 66.3686 60.6556 64.7423C62.282 63.1159 63.1957 60.9101 63.1957 58.61C63.1957 56.31 62.282 54.1042 60.6556 52.4778C59.0292 50.8514 56.8234 49.9377 54.5234 49.9377H40.7972C40.478 49.9403 40.1614 49.8797 39.8657 49.7593C39.5701 49.639 39.3012 49.4613 39.0745 49.2365C38.8479 49.0117 38.668 48.7442 38.5452 48.4495C38.4225 48.1549 38.3593 47.8388 38.3593 47.5196C38.3593 47.2003 38.4225 46.8843 38.5452 46.5896C38.668 46.2949 38.8479 46.0275 39.0745 45.8027C39.3012 45.5779 39.5701 45.4002 39.8657 45.2798C40.1614 45.1594 40.478 45.0988 40.7972 45.1014H50.753C50.8296 45.1014 50.9061 45.0966 50.9826 45.0897C51.0048 45.0878 51.0267 45.0856 51.0488 45.083C51.1111 45.0759 51.173 45.0664 51.2347 45.0545C51.2507 45.0517 51.2652 45.0496 51.2804 45.0464C51.3476 45.0323 51.4142 45.0144 51.4802 44.995C51.5073 44.987 51.5338 44.979 51.5602 44.9694C51.6063 44.9534 51.6519 44.9374 51.6973 44.9192C51.7215 44.9096 51.7453 44.9008 51.7695 44.8904C51.8325 44.8632 51.8946 44.8333 51.9557 44.8008C51.9767 44.7896 51.997 44.7771 52.0176 44.7653C52.0458 44.7493 52.0744 44.7341 52.1022 44.7173C55.4271 42.6547 59.1781 38.9697 62.136 34.8602C63.9437 32.3485 66.0791 28.8229 67.3823 24.7801L74.8812 22.993L74.8807 71.3802ZM50.7523 27.4897C52.476 27.4896 54.1609 26.9785 55.5941 26.0208C57.0273 25.0632 58.1443 23.7021 58.8038 22.1096C59.4634 20.5172 59.636 18.7649 59.2997 17.0743C58.9634 15.3838 58.1334 13.8309 56.9146 12.6121C55.6957 11.3933 54.1429 10.5633 52.4523 10.2271C50.7618 9.89082 49.0095 10.0634 47.4171 10.723C45.8246 11.3827 44.4635 12.4997 43.5059 13.9329C42.5483 15.366 42.0372 17.051 42.0372 18.7746C42.0398 21.0852 42.9589 23.3004 44.5927 24.9342C46.2265 26.5681 48.4418 27.4871 50.7523 27.4897ZM50.7523 15.179C51.4635 15.179 52.1587 15.3899 52.75 15.785C53.3412 16.1801 53.8021 16.7417 54.0742 17.3987C54.3463 18.0558 54.4175 18.7787 54.2788 19.4762C54.14 20.1737 53.7975 20.8144 53.2947 21.3172C52.7918 21.8201 52.1511 22.1625 51.4536 22.3012C50.7561 22.44 50.0332 22.3688 49.3761 22.0966C48.7191 21.8245 48.1576 21.3636 47.7625 20.7723C47.3674 20.181 47.1565 19.4858 47.1565 18.7746C47.1575 17.8213 47.5367 16.9073 48.2108 16.2332C48.885 15.5591 49.799 15.1799 50.7523 15.179ZM27.7965 47.7447C28.0212 47.3556 28.1394 46.9142 28.1394 46.4649C28.1394 46.0156 28.0212 45.5742 27.7965 45.1851L20.4515 32.4632C20.2269 32.0741 19.9038 31.751 19.5146 31.5263C19.1255 31.3017 18.6841 31.1834 18.2348 31.1834C17.7855 31.1834 17.3441 31.3017 16.955 31.5263C16.5658 31.751 16.2427 32.0741 16.018 32.4632L8.6731 45.1851C8.44844 45.5742 8.33017 46.0156 8.33017 46.4649C8.33016 46.9142 8.44843 47.3556 8.67308 47.7447C8.89773 48.1338 9.22085 48.457 9.60995 48.6816C9.99906 48.9063 10.4405 49.0246 10.8898 49.0246H25.5797C26.029 49.0246 26.4704 48.9063 26.8596 48.6817C27.2487 48.457 27.5718 48.1339 27.7965 47.7447ZM15.3233 43.9052L18.2349 38.8624L21.1465 43.9052H15.3233Z"
          fill="url(#paint0_linear_31_323)"
        />
        <defs>
          <linearGradient
            id="paint0_linear_31_323"
            x1="0"
            y1="40.9546"
            x2="80"
            y2="40.9546"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#00B09B" />
            <stop offset="1" stopColor="#96C93D" />
          </linearGradient>
        </defs>
      </svg>
    ),
  },
  {
    titleGE: "სამართალდარღვევები",
    titleEN: "Offences",
    route: "/statistics/offences",
    svg: (
      <svg
        width="80"
        height="80"
        viewBox="0 0 80 80"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M23.5938 28.2812H28.2812V32.9688H23.5938V28.2812Z"
          fill="url(#paint0_linear_31_324)"
        />
        <path
          d="M51.7188 18.9062H56.4062V23.5938H51.7188V18.9062Z"
          fill="url(#paint1_linear_31_324)"
        />
        <path
          d="M70.625 24.9095V15.5917L64.5645 9.53125H56.4062V7.03125C56.4062 3.15422 53.252 0 49.375 0H30.625C26.748 0 23.5938 3.15422 23.5938 7.03125V18.9062H15.5917L9.53125 24.9667V34.285C3.63672 39.1614 0 46.2948 0 54.0625C0 68.3153 11.687 80 25.9375 80C34.3952 80 41.9141 75.8812 46.6181 69.5331C49.0228 70.258 51.5191 70.625 54.0625 70.625C68.3153 70.625 80 58.938 80 44.6875C80 38.1747 77.5638 32.0586 73.1948 27.3388C72.7891 26.7722 71.142 25.337 70.625 24.9095ZM65.9375 17.5333V27.2102C66.5239 27.6792 67.8816 28.5623 69.5086 30.2653C70.0211 31.1605 69.898 32.323 69.1344 33.0866C68.2202 34.0006 66.7336 34.0003 65.8197 33.0866L65.7759 33.1303C65.7586 33.1131 65.7419 33.0955 65.7245 33.0784C62.5834 29.985 58.4417 28.2812 54.0625 28.2812C49.5789 28.2812 45.3966 30.0777 42.3438 33.2078C42.3438 31.5684 42.3438 19.2086 42.3438 17.5334L45.6583 14.2188H62.623L65.9375 17.5333ZM45.1981 37.0286C47.4106 34.4598 50.6123 32.9688 54.0625 32.9688C60.6103 32.9688 65.9375 38.2258 65.9375 44.6875C65.9375 51.1244 60.4994 56.5625 54.0625 56.5625C53.235 56.5625 52.4192 56.4742 51.6214 56.3045C51.6845 55.5653 51.7188 54.8178 51.7188 54.0625C51.7188 47.6933 49.3872 41.7042 45.1981 37.0286ZM28.2812 7.03125C28.2812 5.73891 29.3327 4.6875 30.625 4.6875H49.375C50.6673 4.6875 51.7188 5.73891 51.7188 7.03125V9.53125H43.7167L37.6562 15.5917V20.2792L36.2833 18.9062H28.2812V7.03125ZM25.9375 75.3125C14.2203 75.3125 4.6875 65.7798 4.6875 54.0625C4.6875 42.4622 13.9462 36.8983 14.2188 36.5852V26.9083L17.5333 23.5938H34.3417L37.6562 26.9083V36.5852C38.2427 37.0542 39.6003 37.9373 41.2273 39.6403C41.7398 40.5355 41.6167 41.698 40.8531 42.4616C39.9391 43.3756 38.4525 43.3755 37.5384 42.4616L37.533 42.467C34.5623 39.4963 30.4608 37.6562 25.9375 37.6562C16.8673 37.6562 9.375 45.018 9.375 54.0625C9.375 63.1033 16.8628 70.625 25.9375 70.625C34.9705 70.625 42.3438 63.1428 42.3438 54.0625C42.3438 51.7663 41.8688 49.5792 41.0131 47.593C42.1694 47.2856 43.2625 46.6813 44.1675 45.7761C44.4428 45.5008 44.6892 45.2073 44.9094 44.9008C46.292 47.7148 47.0312 50.8295 47.0312 54.0625C47.0312 65.7798 37.5686 75.3125 25.9375 75.3125ZM37.6562 54.0625C37.6562 55.2414 37.4834 56.3797 37.1662 57.4555C34.4723 53.818 32.9702 49.3223 32.9688 44.6941C35.8131 46.8341 37.6562 50.2369 37.6562 54.0625ZM34.7328 61.8972C32.5836 64.3717 29.4386 65.9375 25.9375 65.9375C19.5006 65.9375 14.0625 60.4994 14.0625 54.0625C14.0625 47.6008 19.3897 42.3438 25.9375 42.3438C26.7702 42.3438 27.5823 42.4325 28.3663 42.5984C28.3097 43.2928 28.2812 43.9908 28.2812 44.6875C28.2812 51.0447 30.6116 57.2042 34.7328 61.8972ZM54.0625 65.9375C52.3902 65.9375 50.7436 65.7431 49.1416 65.3602C49.8241 63.947 50.3817 62.4622 50.8022 60.9208C51.8698 61.137 52.9591 61.25 54.0625 61.25C63.1033 61.25 70.625 53.7622 70.625 44.6875C70.625 42.4308 70.1605 40.2412 69.2778 38.2225C70.4402 37.9166 71.5394 37.3106 72.4487 36.4011C72.7241 36.1258 72.9705 35.8323 73.1906 35.5258C74.5733 38.3398 75.3125 41.4545 75.3125 44.6875C75.3125 56.4048 65.7797 65.9375 54.0625 65.9375Z"
          fill="url(#paint2_linear_31_324)"
        />
        <defs>
          <linearGradient
            id="paint0_linear_31_324"
            x1="23.5938"
            y1="30.625"
            x2="28.2812"
            y2="30.625"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#44A08D" />
            <stop offset="1" stopColor="#093637" />
          </linearGradient>
          <linearGradient
            id="paint1_linear_31_324"
            x1="51.7188"
            y1="21.25"
            x2="56.4062"
            y2="21.25"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#44A08D" />
            <stop offset="1" stopColor="#093637" />
          </linearGradient>
          <linearGradient
            id="paint2_linear_31_324"
            x1="0"
            y1="40"
            x2="80"
            y2="40"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#44A08D" />
            <stop offset="1" stopColor="#093637" />
          </linearGradient>
        </defs>
      </svg>
    ),
  },
  {
    titleGE: "ეროვნული ანგარიშები",
    titleEN: "National Accounts",
    route: "/statistics/national-accounts",
    svg: (
      <svg
        width="66"
        height="66"
        viewBox="0 0 66 66"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M54.4286 3H11.5714C6.83756 3 3 6.83756 3 11.5714V54.4286C3 59.1624 6.83756 63 11.5714 63H54.4286C59.1624 63 63 59.1624 63 54.4286V11.5714C63 6.83756 59.1624 3 54.4286 3Z"
          stroke="url(#paint0_linear_586_4396)"
          strokeWidth="5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M41.5713 50.1428V28.7142"
          stroke="url(#paint1_linear_0_1)"
          strokeWidth="5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M50.1431 50.1429V15.8572"
          stroke="url(#paint2_linear_0_1)"
          strokeWidth="5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M15.8569 50.1428V41.5714"
          stroke="url(#paint3_linear_0_1)"
          strokeWidth="5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M24.4287 33V50.1429"
          stroke="url(#paint4_linear_0_1)"
          strokeWidth="5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M33 37.2856V50.1428"
          stroke="url(#paint5_linear_0_1)"
          strokeWidth="5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <defs>
          <linearGradient
            id="paint1_linear_0_1"
            x1="42.0713"
            y1="28.7142"
            x2="42.0713"
            y2="50.1428"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#266529" />
            <stop offset="1" stopColor="#288A76" />
          </linearGradient>
          <linearGradient
            id="paint2_linear_0_1"
            x1="50.6431"
            y1="15.8572"
            x2="50.6431"
            y2="50.1429"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#266529" />
            <stop offset="1" stopColor="#288A76" />
          </linearGradient>
          <linearGradient
            id="paint3_linear_0_1"
            x1="16.3569"
            y1="41.5714"
            x2="16.3569"
            y2="50.1428"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#266529" />
            <stop offset="1" stopColor="#288A76" />
          </linearGradient>
          <linearGradient
            id="paint4_linear_0_1"
            x1="24.9287"
            y1="33"
            x2="24.9287"
            y2="50.1429"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#266529" />
            <stop offset="1" stopColor="#288A76" />
          </linearGradient>
          <linearGradient
            id="paint5_linear_0_1"
            x1="33.5"
            y1="37.2856"
            x2="33.5"
            y2="50.1428"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#266529" />
            <stop offset="1" stopColor="#288A76" />
          </linearGradient>
          <linearGradient
            id="paint0_linear_586_4396"
            x1="33"
            y1="3"
            x2="33"
            y2="63"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#266529" />
            <stop offset="1" stopColor="#288A76" />
          </linearGradient>
        </defs>
      </svg>
    ),
  },
];

const PlaceholderSvg = () => (
  <svg
    viewBox="0 0 80 80"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className="w-16 h-16"
  >
    <circle
      cx="40"
      cy="40"
      r="38"
      stroke="#0066e0"
      strokeWidth="2"
      fill="#e8f0fe"
    />
    <rect
      x="25"
      y="30"
      width="10"
      height="20"
      rx="2"
      fill="#0066e0"
      opacity="0.5"
    />
    <rect
      x="40"
      y="22"
      width="10"
      height="28"
      rx="2"
      fill="#0066e0"
      opacity="0.7"
    />
    <rect x="55" y="36" width="10" height="14" rx="2" fill="#0066e0" />
  </svg>
);

const StatCards = ({ language }) => {
  const [hoveredIndex, setHoveredIndex] = React.useState(null);
  const navigate = useNavigate();
  return (
    <div className="mt-6 grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-6">
      {statCardsData.map((card, i) => (
        <div
          key={i}
          className="flex flex-col items-center justify-center gap-4 rounded-2xl cursor-pointer"
          onMouseEnter={() => setHoveredIndex(i)}
          onMouseLeave={() => setHoveredIndex(null)}
          onClick={() => card.route && navigate(card.route)}
          style={{
            border:
              hoveredIndex === i ? "1px solid #0066e0" : "1px solid #e5e7eb",
            padding: "24px 16px",
            background: "#fff",
            transform: hoveredIndex === i ? "scale(1.05)" : "scale(1)",
            transition: "border-color 0.2s, transform 0.2s, box-shadow 0.2s",
            boxShadow:
              hoveredIndex === i ? "0 4px 16px rgba(0,102,224,0.12)" : "none",
          }}
        >
          {card.svg ? card.svg : <PlaceholderSvg />}
          <p
            className="text-center font-semibold"
            style={{ fontSize: "14px", color: "#1a1a1a" }}
          >
            {language === "EN" ? card.titleEN : card.titleGE}
          </p>
          <p
            style={{
              fontSize: "12px",
              color: "#0066e0",
              fontWeight: "500",
              opacity: hoveredIndex === i ? 1 : 0,
              transform:
                hoveredIndex === i ? "translateY(0)" : "translateY(4px)",
              transition: "opacity 0.2s, transform 0.2s",
            }}
          >
            <span
              style={{
                display: "inline-block",
                width: "16px",
                height: "2px",
                background: "#0066e0",
                verticalAlign: "middle",
                marginRight: "6px",
                borderRadius: "1px",
              }}
            />
            {language === "EN" ? "View" : "ნახვა"}
          </p>
        </div>
      ))}
    </div>
  );
};

const Hero = ({ currentSlide, prev, next, goTo, current, language }) => {
  const isMobile = useWindowWidth() < 768;
  const [lifeCalcOpen, setLifeCalcOpen] = React.useState(false);
  return (
    <>
    {lifeCalcOpen && (
      <div
        onClick={() => setLifeCalcOpen(false)}
        style={{
          position: "fixed",
          inset: 0,
          background: "rgba(0,0,0,0.6)",
          zIndex: 9999,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <div
          onClick={(e) => e.stopPropagation()}
          style={{
            background: "#fff",
            borderRadius: "16px",
            overflow: "hidden",
            width: "90vw",
            maxWidth: "900px",
            height: "80vh",
            display: "flex",
            flexDirection: "column",
          }}
        >
          <div style={{ display: "flex", justifyContent: "flex-end", padding: "8px 12px" }}>
            <button
              onClick={() => setLifeCalcOpen(false)}
              aria-label="Close"
              style={{
                background: "transparent",
                border: "none",
                fontSize: "22px",
                cursor: "pointer",
                color: "#333",
              }}
            >
              ✕
            </button>
          </div>
          <iframe
            src={`https://database.geostat.ge/pyramid/life.php?lang=${language === "EN" ? "en" : "ka"}`}
            style={{ flex: 1, border: "none", width: "100%" }}
            title="Life Expectancy Calculator"
          />
        </div>
      </div>
    )}
    <section
      className={`w-full rounded-[30px] p-4 md:p-10 flex gap-8 items-center ${isMobile ? "flex-col" : "justify-between"}`}
    >
      {/* CENTER IMAGE + OVERLAY TEXT */}
      <div
        className={`relative flex-shrink-0 rounded-[40px] overflow-hidden ${isMobile ? "w-full h-[280px]" : "w-[65%] h-[500px]"}`}
      >
        <div
          className="w-full h-full rounded-[40px]"
          style={{
            backgroundImage: `url(${slides[currentSlide].src})`,
            backgroundSize: "cover",
            backgroundPosition: "center",
            transition: "background-image 0.7s ease",
          }}
        />

        {/* dark gradient for text readability */}
        <div className="absolute inset-0 rounded-[40px] bg-gradient-to-b from-black/10 via-transparent to-black/50 pointer-events-none"></div>

        {/* LEFT TEXT overlaid on image */}
        <div className="absolute bottom-12 left-6 right-6 z-10">
          <p
            className="text-white/70 mb-2 flex items-center gap-2"
            style={{ fontSize: "16px", fontWeight: "bold" }}
          >
            📊 {language === "EN" ? "INFOGRAPHICS" : "ინფოგრაფიკა"}
          </p>
          <h1
            className="text-[36px] leading-[1.1] font-bold mb-4 drop-shadow uppercase"
            style={{ color: "#0066e0" }}
          >
            {(language === "EN"
              ? slides[currentSlide].titleEN
              : slides[currentSlide].titleGE
            )
              .split("\n")
              .map((line, i) => (
                <span key={i}>
                  {line}
                  {i === 0 && <br />}
                </span>
              ))}
          </h1>
          <button
            className="flex items-center overflow-hidden rounded-full cursor-pointer"
            style={{ border: "1px solid #0066e0", background: "transparent" }}
            onClick={() =>
              window.open(
                `/coverpdf/${language === "EN" ? "eng" : "geo"}/${currentSlide + 1}.pdf`,
                "_blank",
              )
            }
          >
            <span
              className="px-5 py-1.5 font-medium"
              style={{ color: "#0066e0", fontSize: "16px", fontWeight: "bold" }}
            >
              {language === "EN" ? "See more" : "ნახვა"}
            </span>
            <span
              className="text-white flex items-center justify-center"
              style={{
                background: "#0066e0",
                fontSize: "20px",
                fontWeight: "bold",
                width: "42px",
                height: "42px",
              }}
            >
              ›
            </span>
          </button>
        </div>

        {/* Dots */}
        <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-2 z-10">
          {slides.map((_, i) => (
            <button
              key={i}
              onClick={() => goTo(i)}
              className="w-3 h-3 rounded-full cursor-pointer transition-all"
              style={{
                background: i === current ? "#fff" : "rgba(255,255,255,0.4)",
              }}
              aria-label={`სლაიდი ${i + 1}`}
            />
          ))}
        </div>
      </div>

      {/* RIGHT CARDS */}
      <div
        className={`flex gap-6 ${isMobile ? "w-full flex-row h-[200px]" : "flex-col h-[500px] w-[30%]"}`}
      >
        {/* CARD 1 */}
        <a
          href="https://pyramid.geostat.ge/ka/marriages"
          className="flex-1 rounded-[25px] overflow-hidden relative block"
        >
          <img
            src={marriageImg}
            alt="ქორწინების კალკულატორი"
            className="w-full h-full object-cover"
          />
          <div
            className="absolute bottom-0 w-full text-white text-sm text-center"
            style={{ background: "rgba(54, 55, 149, 0.8)", padding: "15px" }}
          >
            {language === "EN"
              ? "MARRIAGE CALCULATOR"
              : "ქორწინების კალკულატორი"}
          </div>
        </a>

        {/* CARD 2 */}
        <div
          className="flex-1 rounded-[25px] overflow-hidden relative block cursor-pointer"
          onClick={() => setLifeCalcOpen(true)}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => e.key === "Enter" && setLifeCalcOpen(true)}
        >
          <img
            src={lifeCalcImg}
            alt="სოციალური მონაცემების კალკულატორი"
            className="w-full h-full object-cover"
          />
          <div
            className="absolute bottom-0 w-full text-white text-sm text-center"
            style={{ background: "rgba(54, 55, 149, 0.8)", padding: "15px" }}
          >
            {language === "EN"
              ? "LIFE EXPECTANCY CALCULATOR"
              : "სიცოცხლის მოსალოდნელი ხანგრძლივობის კალკულატორი"}
          </div>
        </div>
      </div>
    </section>
    </>
  );
};

const Main = ({ language = "GE" }) => {
  const [current, setCurrent] = useState(0);
  const intervalRef = useRef(null);
  const location = useLocation();
  const txt = t[language] ?? t.GE;

  const startAutoPlay = () => {
    intervalRef.current = setInterval(() => {
      setCurrent((prev) => (prev + 1) % slides.length);
    }, 8000);
  };

  useEffect(() => {
    startAutoPlay();
    return () => clearInterval(intervalRef.current);
  }, []);

  useEffect(() => {
    if (!location.hash) return;
    const targetId = location.hash.replace("#", "");
    const timer = setTimeout(() => {
      const section = document.getElementById(targetId);
      if (section) {
        section.scrollIntoView({ behavior: "smooth" });
      }
    }, 0);

    return () => clearTimeout(timer);
  }, [location.hash]);

  const goTo = (index) => {
    setCurrent(index);
    clearInterval(intervalRef.current);
    startAutoPlay();
  };

  const prev = () => goTo((current - 1 + slides.length) % slides.length);
  const next = () => goTo((current + 1) % slides.length);

  return (
    <main
      className="w-full font-firago"
      style={{ fontFeatureSettings: '"case" on' }}
    >
      {/* ── HERO ── */}
      <div className="px-6 md:px-16 py-10">
        <Hero
          currentSlide={current}
          prev={prev}
          next={next}
          goTo={goTo}
          current={current}
          language={language}
        />
      </div>

      {/* ── SECTIONS ── */}
      {txt.sections.map((title, idx) => {
        const sectionIds = [
          "statistics",
          "publications",
          "sdg",
          "legislation",
          "links",
        ];
        return (
          <section
            key={idx}
            id={sectionIds[idx]}
            className={`w-full ${idx === 1 ? "" : "py-10 px-6 md:px-16"}`}
            style={{ borderBottom: "1px solid #e5e7eb" }}
          >
            {idx === 1 ? (
              <Publications language={language} />
            ) : (
              <>
                <SectionTitle title={title} />
                {idx === 0 ? (
                  <StatCards language={language} />
                ) : idx === 2 ? (
                  <SDGSection language={language} />
                ) : idx === 3 ? (
                  <Legislation language={language} />
                ) : idx === 4 ? (
                  <Links language={language} />
                ) : (
                  <div className="mt-4 text-gray-400 text-sm italic text-center">
                    {language === "GE"
                      ? "კონტენტი მალე დაემატება..."
                      : "Content coming soon..."}
                  </div>
                )}
              </>
            )}
          </section>
        );
      })}
    </main>
  );
};

export default Main;
