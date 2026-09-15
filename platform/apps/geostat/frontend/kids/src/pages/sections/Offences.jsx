import React from "react";
import slideImg from "../../assets/images/slide-9.png";
import SectionDataPage from "../../components/SectionDataPage";

const Offences = ({ language }) => {
  return (
    <SectionDataPage
      language={language}
      sectionKey="offences"
      slideImg={slideImg}
      titleEN="Crime"
      titleGE="სამართალდარღვევები"
      coverPdfNumber={9}
    />
  );
};

export default Offences;
