import React from "react";
import slideImg from "../../assets/images/slide-3.png";
import SectionDataPage from "../../components/SectionDataPage";

const Education = ({ language }) => {
  return (
    <SectionDataPage
      language={language}
      sectionKey="education"
      slideImg={slideImg}
      titleEN="Education"
      titleGE="განათლება"
      coverPdfNumber={3}
    />
  );
};

export default Education;
