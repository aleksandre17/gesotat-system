import React from "react";
import slideImg from "../../assets/images/slide-2.jpg";
import SectionDataPage from "../../components/SectionDataPage";

const HealthCare = ({ language }) => {
  return (
    <SectionDataPage
      language={language}
      sectionKey="health-care"
      slideImg={slideImg}
      titleEN="Health Care"
      titleGE="ჯანმრთელობის დაცვა"
      coverPdfNumber={2}
    />
  );
};

export default HealthCare;
