import React from "react";
import slideImg from "../../assets/images/slide-4.jpg";
import SectionDataPage from "../../components/SectionDataPage";

const SocialProtection = ({ language }) => {
  return (
    <SectionDataPage
      language={language}
      sectionKey="social-protection"
      slideImg={slideImg}
      titleEN="Social Protection"
      titleGE="სოციალური უზრუნველყოფა"
      coverPdfNumber={4}
    />
  );
};

export default SocialProtection;
