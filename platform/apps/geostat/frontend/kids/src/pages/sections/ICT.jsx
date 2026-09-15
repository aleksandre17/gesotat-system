import React from "react";
import slideImg from "../../assets/images/slide-7.png";
import SectionDataPage from "../../components/SectionDataPage";

const ICT = ({ language }) => {
  return (
    <SectionDataPage
      language={language}
      sectionKey="ict"
      slideImg={slideImg}
      titleEN="ICT"
      titleGE="საინფორმაციო და საკომუნიკაციო ტექნოლოგიები"
      coverPdfNumber={7}
    />
  );
};

export default ICT;
