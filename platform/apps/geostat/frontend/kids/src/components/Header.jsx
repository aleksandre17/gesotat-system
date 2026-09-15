import React, { useState, useRef, useEffect } from "react";
import { Link } from "react-router-dom";
import { useLocation, useNavigate } from "react-router-dom";
import sakstatLogoGe from "../assets/images/sakstat-logo.svg";
import sakstatLogoEn from "../assets/images/sakstat-logo-en.png";
import headerLogo1 from "../assets/images/header-logo-1.png";
import headerGif from "../assets/images/header.gif";
import headerGifEn from "../assets/images/header-en.gif";
import georgianFlag from "../assets/images/georgian-flag.svg";
import britishFlag from "../assets/images/british-flag.png";
import facebookIcon from "../assets/images/facebook.svg";
import twitterIcon from "../assets/images/twitter.svg";
import linkedinIcon from "../assets/images/linkedin.svg";
import headerBg from "../assets/images/header-bg.jpg";
import processIcon from "../assets/images/process.png";
import compliantIcon from "../assets/images/compliant.png";
import constructionIcon from "../assets/images/construction.png";

const Header = ({ language = "GE", setLanguage = () => {}, onGlossaryOpen = () => {} }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const [langOpen, setLangOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [statsOpen, setStatsOpen] = useState(false);
  const [mobileStatsOpen, setMobileStatsOpen] = useState(false);
  const langRef = useRef(null);

  const sections = [
    { ge: "ძირითადი მაჩვენებელი", en: "Main Indicators", route: "/statistics/main-indicators" },
    { ge: "მოსახლეობა", en: "Population", route: "/statistics/population" },
    { ge: "ჯანმრთელობის დაცვა", en: "Health Care", route: "/statistics/health-care" },
    { ge: "განათლება", en: "Education", route: "/statistics/education" },
    { ge: "სოციალური უზრუნველყოფა", en: "Social Protection", route: "/statistics/social-protection" },
    { ge: "ცხოვრების დონე", en: "Living Standards", route: "/statistics/living-standards" },
    { ge: "დასაქმება და უმუშევრობა", en: "Employment and Unemployment", route: "/statistics/employment" },
    { ge: "საინფ. და საკომ. ტექნოლოგიები", en: "ICT", route: "/statistics/ict" },
    { ge: "ტურიზმი", en: "Tourism", route: "/statistics/tourism" },
    { ge: "სამართალდარღვევები", en: "Crime", route: "/statistics/offences" },
    { ge: "ეროვნული ანგარიშები", en: "GDP and National Accounts", route: "/statistics/national-accounts" },
  ];

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (langRef.current && !langRef.current.contains(e.target)) {
        setLangOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const languages = [
    { code: "GE", label: "ქარ", flag: georgianFlag },
    { code: "EN", label: "ENG",  flag: britishFlag  },
  ];

  const current = languages.find((l) => l.code === language);

  const t = {
    GE: {
      portalLine1: "ბავშვების და მოზარდების",
      portalLine2: "სტატისტიკის პორტალი",
      nav: [
        "სტატისტიკური ინფორმაცია",
        "პუბლიკაციები",
        "კანონმდებლობა",
        "ბმულები",
        "გლოსარიუმი",
        "ინფოგრაფიკა",
      ],
    },
    EN: {
      portalLine1: "STATISTICAL PORTAL",
      portalLine2: "ON CHILDREN AND YOUTH",
      nav: [
        "MAIN STATISTICS",
        "PUBLICATIONS",
        "LEGISLATION",
        "LINKS",
        "GLOSSARY",
        "INFOGRAPHIC",
      ],
    },
  };

  const txt = t[language] ?? t.GE;

  const goToHomeSection = (sectionId) => {
    if (location.pathname === "/") {
      const section = document.getElementById(sectionId);
      if (section) {
        section.scrollIntoView({ behavior: "smooth" });
      }
      return;
    }
    navigate(`/#${sectionId}`);
  };

  return (
    <header className="w-full font-firago" style={{ fontFeatureSettings: '"case" on' }}>
      {/* TOP BAR */}
      <div className="flex items-center justify-between px-4 md:px-10 py-3 bg-gray-100">

        {/* LEFT */}
        <div className="flex items-center gap-4">
          <Link to="/" className="cursor-pointer">
            <img
              src={language === "GE" ? sakstatLogoGe : sakstatLogoEn}
              alt="Sakstat Logo"
              className="h-[45px] w-auto object-contain"
            />
          </Link>

          <Link to="/" className="leading-tight cursor-pointer hidden sm:block" style={{ textDecoration: "none", color: "#3d3d3e", fontFeatureSettings: '"cpsp" on, "case" on', fontSize: "14px", fontWeight: "bold" }}>
            <p>{txt.portalLine1}</p>
            <p>{txt.portalLine2}</p>
          </Link>

          <img
            src={headerLogo1}
            alt="Header Logo"
            className="h-[80%] w-auto object-contain"
          />
        </div>

        {/* RIGHT — desktop */}
        <div className="hidden md:flex flex-col items-end gap-2">
          <div className="flex items-center gap-4">
            {/* Junior Portal GIF */}
            <a href="https://juniors.geostat.ge" target="_blank" rel="noreferrer">
              <img src={language === "GE" ? headerGif : headerGifEn} alt="Juniors" className="h-[140px] w-auto object-contain" />
            </a>

            {/* Language Dropdown */}
            <div className="relative" ref={langRef}>
              <button
                onClick={() => setLangOpen((prev) => !prev)}
                className="flex items-center gap-2 border border-gray-300 rounded-full px-3 py-1 text-sm bg-white hover:bg-gray-50 cursor-pointer"
              >
                <img src={current.flag} alt={current.code} className="w-5 h-4 object-cover rounded-sm" />
                <span>{current.label}</span>
                <svg className={`w-3 h-3 transition-transform ${langOpen ? "rotate-180" : ""}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              {langOpen && (
                <div className="absolute right-0 mt-1 w-36 bg-white border border-gray-200 rounded-lg shadow-lg z-50 overflow-hidden">
                  {languages.map((lang) => (
                    <button
                      key={lang.code}
                      onClick={() => { setLanguage(lang.code); setLangOpen(false); }}
                      className={`flex items-center gap-2 w-full px-3 py-2 text-sm hover:bg-gray-100 cursor-pointer ${
                        language === lang.code ? "bg-blue-50 font-semibold text-blue-700" : ""
                      }`}
                    >
                      <img src={lang.flag} alt={lang.code} className="w-5 h-4 object-cover rounded-sm" />
                      <span>{lang.label}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Socials */}
            <div className="flex items-center gap-3">
              <a href="https://www.facebook.com/geostat.ge/" target="_blank" rel="noreferrer" aria-label="Facebook">
                <img src={facebookIcon} alt="Facebook" className="w-4 h-4 object-contain" />
              </a>
              <a href="https://twitter.com/Geostat100" target="_blank" rel="noreferrer" aria-label="Twitter">
                <img src={twitterIcon} alt="Twitter" className="w-4 h-4 object-contain" />
              </a>
              <a href="https://www.linkedin.com/company/national-statistics-office-of-georgia/mycompany/" target="_blank" rel="noreferrer" aria-label="LinkedIn">
                <img src={linkedinIcon} alt="LinkedIn" className="w-4 h-4 object-contain" />
              </a>
            </div>
          </div>
        </div>

        {/* HAMBURGER — mobile */}
        <button
          className="md:hidden flex flex-col justify-center items-center gap-[5px] p-2 cursor-pointer"
          onClick={() => setMenuOpen((prev) => !prev)}
          aria-label="მენიუ"
        >
          <span className={`block w-6 h-[2px] bg-gray-700 transition-transform duration-300 ${menuOpen ? "rotate-45 translate-y-[7px]" : ""}`} />
          <span className={`block w-6 h-[2px] bg-gray-700 transition-opacity duration-300 ${menuOpen ? "opacity-0" : ""}`} />
          <span className={`block w-6 h-[2px] bg-gray-700 transition-transform duration-300 ${menuOpen ? "-rotate-45 -translate-y-[7px]" : ""}`} />
        </button>
      </div>

      {/* MOBILE MENU */}
      {menuOpen && (
        <div className="md:hidden bg-gray-100 px-6 pb-4 flex flex-col gap-3">
          {/* GIF — mobile */}
          <a href="https://juniors.geostat.ge" target="_blank" rel="noreferrer" className="pt-3">
            <img src={language === "GE" ? headerGif : headerGifEn} alt="Juniors" className="h-[100px] w-auto object-contain" />
          </a>
          {/* Language */}
          <div className="flex items-center gap-3 pt-3">
            {languages.map((lang) => (
              <button
                key={lang.code}
                onClick={() => { setLanguage(lang.code); setMenuOpen(false); }}
                className={`flex items-center gap-2 px-3 py-1 rounded-full border text-sm cursor-pointer ${
                  language === lang.code ? "bg-blue-600 text-white border-blue-600" : "bg-white border-gray-300"
                }`}
              >
                <img src={lang.flag} alt={lang.code} className="w-5 h-4 object-cover rounded-sm" />
                <span>{lang.label}</span>
              </button>
            ))}
          </div>
          {/* Nav links */}
          {txt.nav.map((item, idx) =>
            idx === 0 ? (
              <div key={item}>
                <button
                  onClick={() => setMobileStatsOpen((prev) => !prev)}
                  className="flex items-center justify-between w-full text-[15px] text-gray-800 py-2 border-b border-gray-200 hover:text-blue-700 cursor-pointer"
                >
                  {item}
                  <svg className={`w-3 h-3 transition-transform ${mobileStatsOpen ? "rotate-180" : ""}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                  </svg>
                </button>
                {mobileStatsOpen && (
                  <div className="pl-4 flex flex-col">
                    {sections.map((s) => (
                      <Link
                        key={s.route}
                        to={s.route}
                        onClick={() => { setMenuOpen(false); setMobileStatsOpen(false); }}
                        className="text-[14px] text-gray-700 py-2 border-b border-gray-100 hover:text-blue-700"
                        style={{ fontFamily: "FiraGO", fontFeatureSettings: '"case" on' }}
                      >
                        {language === "EN" ? s.en : s.ge}
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            ) : (
              <a
                key={item}
                href={idx === 1 ? "#publications" : idx === 2 ? "#legislation" : idx === 3 ? "#links" : "#"}
                onClick={(e) => {
                  e.preventDefault();
                  if (idx === 1 || idx === 2 || idx === 3) {
                    const ids = { 1: "publications", 2: "legislation", 3: "links" };
                    goToHomeSection(ids[idx]);
                    setMenuOpen(false);
                  } else if (idx === 4) {
                    onGlossaryOpen();
                    setMenuOpen(false);
                  } else if (idx === 5) {
                    navigate("/infographic");
                    setMenuOpen(false);
                  } else {
                    setMenuOpen(false);
                  }
                }}
                className="text-[15px] text-gray-800 py-2 border-b border-gray-200 hover:text-blue-700"
              >
                {item}
              </a>
            )
          )}
          {/* Socials */}
          <div className="flex items-center gap-4 pt-2">
            <a href="https://www.facebook.com/geostat.ge/" target="_blank" rel="noreferrer">
              <img src={facebookIcon} alt="Facebook" className="w-5 h-5 object-contain" />
            </a>
            <a href="https://twitter.com/Geostat100" target="_blank" rel="noreferrer">
              <img src={twitterIcon} alt="Twitter" className="w-5 h-5 object-contain" />
            </a>
            <a href="https://www.linkedin.com/company/national-statistics-office-of-georgia/mycompany/" target="_blank" rel="noreferrer">
              <img src={linkedinIcon} alt="LinkedIn" className="w-5 h-5 object-contain" />
            </a>
          </div>
        </div>
      )}

      {/* NAVBAR — desktop only */}
      <nav className="hidden md:block text-white" style={{ background: "#0066e0" }}>
        <div className="flex justify-center items-stretch gap-10 text-[16px]">
          {txt.nav.map((item, idx) =>
            idx === 0 ? (
              <div
                key={item}
                className="relative flex items-stretch"
                onMouseEnter={() => setStatsOpen(true)}
                onMouseLeave={() => setStatsOpen(false)}
              >
                <button className="flex items-center gap-1 px-3 py-[20px] transition-colors duration-200 hover:bg-white hover:text-gray-900 cursor-pointer">
                  {item}
                  <svg className={`w-3 h-3 transition-transform ${statsOpen ? "rotate-180" : ""}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                  </svg>
                </button>
                {statsOpen && (
                  <div className="absolute top-full left-0 bg-white text-gray-800 shadow-xl rounded-b-lg z-50 min-w-[280px] py-1" style={{ border: "1px solid #e5e7eb" }}>
                    {sections.map((s) => (
                      <Link
                        key={s.route}
                        to={s.route}
                        onClick={() => setStatsOpen(false)}
                        className="block px-4 py-2 text-[14px] hover:bg-blue-50 hover:text-blue-700"
                        style={{ fontFamily: "FiraGO", fontFeatureSettings: '"case" on' }}
                      >
                        {language === "EN" ? s.en : s.ge}
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            ) : (
              <a
                key={item}
                href={idx === 1 ? "#publications" : idx === 2 ? "#legislation" : idx === 3 ? "#links" : "#"}
                onClick={(e) => {
                  e.preventDefault();
                  if (idx === 1 || idx === 2 || idx === 3) {
                    const ids = { 1: "publications", 2: "legislation", 3: "links" };
                    goToHomeSection(ids[idx]);
                  } else if (idx === 4) {
                    onGlossaryOpen();
                  } else if (idx === 5) {
                    navigate("/infographic");
                  }
                }}
                className="flex items-center px-3 py-[20px] transition-colors duration-200 hover:bg-white hover:text-gray-900"
              >
                {item}
              </a>
            )
          )}
        </div>
      </nav>
    </header>
  );
};

export default Header;
