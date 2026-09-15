import React from "react";
import slideImg from "../../assets/images/slide-5.png";
import SectionDataPage from "../../components/SectionDataPage";

const LivingStandards = ({ language }) => {
  return (
    <SectionDataPage
      language={language}
      sectionKey="living-standards"
      slideImg={slideImg}
      titleEN="Living Standards"
      titleGE="ცხოვრების დონე"
      coverPdfNumber={5}
    />
  );
};

export default LivingStandards;
