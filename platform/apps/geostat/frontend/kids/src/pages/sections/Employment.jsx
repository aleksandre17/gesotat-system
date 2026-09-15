import React from "react";
import slideImg from "../../assets/images/slide-6.jpg";
import SectionDataPage from "../../components/SectionDataPage";

const Employment = ({ language }) => {
  return (
    <SectionDataPage
      language={language}
      sectionKey="employment"
      slideImg={slideImg}
      titleEN="Employment and Unemployment"
      titleGE="დასაქმება და უმუშევრობა"
      coverPdfNumber={6}
    />
  );
};

export default Employment;
