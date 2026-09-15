import React from "react";
import slideImg from "../../assets/images/slide-1.jpg";
import SectionDataPage from "../../components/SectionDataPage";

const Population = ({ language }) => {
  return (
    <SectionDataPage
      language={language}
      sectionKey="population"
      slideImg={slideImg}
      titleEN="Population"
      titleGE="მოსახლეობა"
      coverPdfNumber={1}
    />
  );
};

export default Population;
