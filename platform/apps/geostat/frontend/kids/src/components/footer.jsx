import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import facebookIcon from "../assets/images/facebook.svg";
import twitterIcon from "../assets/images/twitter.svg";
import linkedinIcon from "../assets/images/linkedin.svg";

const Footer = ({ language = "GE", onGlossaryOpen = () => {} }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const year = new Date().getFullYear();
  const isEN = language === "EN";
  const [showGoTop, setShowGoTop] = useState(false);

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

  useEffect(() => {
    const handleScroll = () => {
      setShowGoTop(window.scrollY > 300);
    };

    window.addEventListener("scroll", handleScroll);
    handleScroll();

    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const handleGoTop = () => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  return (
    <>
      {showGoTop && (
        <button
          onClick={handleGoTop}
          aria-label="Go to top"
          style={{
            position: "fixed",
            right: "24px",
            bottom: "24px",
            zIndex: 1000,
            width: "52px",
            height: "52px",
            borderRadius: "999px",
            border: "none",
            background: "#0066e0",
            color: "#fff",
            boxShadow: "0 10px 30px rgba(0, 102, 224, 0.28)",
            cursor: "pointer",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: "22px",
            fontWeight: 700,
          }}
        >
          ↑
        </button>
      )}

      <footer
        className="bg-[#005c97] text-white font-firago"
        style={{ fontFeatureSettings: '"case" on' }}
      >
        {/* TOP SECTION */}
        <div className="py-12 px-6 flex flex-col md:flex-row md:justify-around gap-10 md:gap-0">
        {/* LEFT */}
        <div>
          <h3 className="font-semibold text-lg mb-4">
            {isEN ? "CONTACT INFORMATION" : "საკონტაქტო ინფორმაცია"}
          </h3>

          <p style={{ fontSize: "12px", fontWeight: 400, lineHeight: "30px" }}>
            {isEN
              ? "National Statistics Office of Georgia"
              : "საქართველოს სტატისტიკის ეროვნული სამსახური"}
          </p>
          <p style={{ fontSize: "12px", fontWeight: 400, lineHeight: "30px" }}>
            <a style={{ color: "white" }} href="tel:+995322367210">
              (+995 32) 236 72 10, (+995 32) 260 11 60
            </a>
          </p>
          <p style={{ fontSize: "12px", fontWeight: 400, lineHeight: "30px" }}>
            <a
              style={{ color: "white" }}
              href="mailto:info@geostat.ge"
              tabIndex={92}
            >
              info@geostat.ge
            </a>
          </p>
          <p
            style={{
              fontSize: "12px",
              fontWeight: 400,
              lineHeight: "30px",
              display: "flex",
              alignItems: "center",
              gap: "6px",
            }}
            className="mb-6"
          >
            <a
              href="https://www.google.com/maps/place/%E1%83%A1%E1%83%90%E1%83%A5%E1%83%90%E1%83%A0%E1%83%97%E1%83%95%E1%83%94%E1%83%9A%E1%83%9D%E1%83%A1+%E1%83%A1%E1%83%A2%E1%83%90%E1%83%A2%E1%83%98%E1%83%A1%E1%83%A2%E1%83%98%E1%83%99%E1%83%98%E1%83%A1+%E1%83%94%E1%83%A0%E1%83%9D%E1%83%95%E1%83%9C%E1%83%A3%E1%83%9A%E1%83%98+%E1%83%A1%E1%83%90%E1%83%9B%E1%83%A1%E1%83%90%E1%83%AE%E1%83%A3%E1%83%A0%E1%83%98/@41.7296526,44.7931013,818m/data=!3m2!1e3!4b1!4m6!3m5!1s0x404472b62853ba09:0xa05d0cb0a0c61544!8m2!3d41.7296487!4d44.7979722!16s%2Fg%2F11hcz12gfm?entry=ttu&g_ep=EgoyMDI2MDQyMC4wIKXMDSoASAFQAw%3D%3D"
              target="_blank"
              rel="noreferrer"
              style={{ color: "white", display: "inline-flex", alignItems: "center", gap: "6px" }}
            >
              {isEN
                ? "30, Tsotne Dadiani Str., 0180, Tbilisi, Georgia"
                : "ცოტნე დადიანის ქ. 30, თბილისი, 0180"}
            </a>
          </p>

          <h4 className="font-semibold mb-3">
            {isEN ? "Social Networks" : "სოციალური ქსელები"}
          </h4>
          <div className="flex gap-4 items-center">
            <a
              href="https://www.facebook.com/geostat.ge/"
              target="_blank"
              rel="noreferrer"
              aria-label="Facebook"
            >
              <img
                src={facebookIcon}
                alt="Facebook"
                className="w-5 h-5 object-contain brightness-0 invert"
              />
            </a>
            <a
              href="https://twitter.com/Geostat100"
              target="_blank"
              rel="noreferrer"
              aria-label="Twitter"
            >
              <img
                src={twitterIcon}
                alt="Twitter"
                className="w-5 h-5 object-contain brightness-0 invert"
              />
            </a>
            <a
              href="https://www.linkedin.com/company/national-statistics-office-of-georgia/mycompany/"
              target="_blank"
              rel="noreferrer"
              aria-label="LinkedIn"
            >
              <img
                src={linkedinIcon}
                alt="LinkedIn"
                className="w-5 h-5 object-contain brightness-0 invert"
              />
            </a>
          </div>
        </div>

        {/* CENTER */}
        <div>
          <h6 className="font-semibold text-lg mb-4">
            {isEN ? "MENU" : "მენიუ"}
          </h6>

          <ul className="space-y-0">
            <li
              className="hover:underline cursor-pointer"
              style={{ fontSize: "12px", fontWeight: 400, lineHeight: "30px" }}
              onClick={() => goToHomeSection("statistics")}
            >
              {isEN ? "MAIN STATISTICS" : "სტატისტიკური ინფორმაცია"}
            </li>
            <li
              className="hover:underline cursor-pointer"
              style={{ fontSize: "12px", fontWeight: 400, lineHeight: "30px" }}
              onClick={() => goToHomeSection("publications")}
            >
              {isEN ? "PUBLICATIONS" : "პუბლიკაციები"}
            </li>
            <li
              className="hover:underline cursor-pointer"
              style={{ fontSize: "12px", fontWeight: 400, lineHeight: "30px" }}
              onClick={() => goToHomeSection("legislation")}
            >
              {isEN ? "LEGISLATION" : "კანონმდებლობა"}
            </li>
            <li
              className="hover:underline cursor-pointer"
              style={{ fontSize: "12px", fontWeight: 400, lineHeight: "30px" }}
              onClick={() => goToHomeSection("links")}
            >
              {isEN ? "LINKS" : "ბმულები"}
            </li>
            <li
              className="hover:underline cursor-pointer"
              style={{ fontSize: "12px", fontWeight: 400, lineHeight: "30px" }}
              onClick={onGlossaryOpen}
            >
              {isEN ? "GLOSSARY" : "გლოსარიუმი"}
            </li>
            <li
              className="hover:underline cursor-pointer"
              style={{ fontSize: "12px", fontWeight: 400, lineHeight: "30px" }}
              onClick={() => navigate("/infographic")}
            >
              {isEN ? "INFOGRAPHIC" : "ინფოგრაფიკა"}
            </li>
          </ul>
        </div>

        <div className="text-center md:text-left">
          <h3 className="font-semibold text-lg mb-4">
            <a
              href="https://www.geostat.ge/ka/page/monacemta-gamoyenebis-pirobebi"
              target="_blank"
              rel="noreferrer"
              style={{ color: "white" }}
            >
              {isEN ? "Terms of Use" : "მონაცემთა გამოყენების პირობები"}
            </a>
          </h3>
        </div>
        </div>

        {/* BOTTOM BAR */}
        <div className="border-t border-white/20 text-center text-sm py-4 relative">
          <p
            style={{
              textAlign: "center",
              fontSize: "0.7rem",
              fontWeight: "normal",
            }}
          >
            {isEN
              ? `All rights reserved © Geostat ${year}`
              : `ყველა უფლება დაცულია © საქსტატი ${year}`}
          </p>
          <p
            style={{
              textAlign: "center",
              fontSize: "0.7rem",
              fontWeight: "normal",
            }}
          >
            {isEN
              ? "The portal was developed with the financial and technical support of the United Nations Childrens Fund (UNICEF)"
              : "პორტალი შეიქმნა გაეროს ბავშვთა ფონდის (UNICEF) ტექნიკური და ფინანსური მხარდაჭერით."}
          </p>
        </div>
      </footer>
    </>
  );
};

export default Footer;
