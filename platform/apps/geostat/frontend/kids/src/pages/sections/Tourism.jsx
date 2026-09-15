import React from "react";
import slideImg from "../../assets/images/slide-8.jpg";
import SectionDataPage from "../../components/SectionDataPage";

const Tourism = ({ language }) => {
  return (
    <SectionDataPage
      language={language}
      sectionKey="tourism"
      slideImg={slideImg}
      titleEN="Tourism"
      titleGE="ტურიზმი"
      coverPdfNumber={8}
    />
  );
};

export default Tourism;
