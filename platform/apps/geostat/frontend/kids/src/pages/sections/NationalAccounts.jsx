import React from "react";
import slideImg from "../../assets/images/slide-10.jpg";
import SectionDataPage from "../../components/SectionDataPage";

const NationalAccounts = ({ language }) => {
  return (
    <SectionDataPage
      language={language}
      sectionKey="national-accounts"
      slideImg={slideImg}
      titleEN="GDP and National Accounts"
      titleGE="ეროვნული ანგარიშები"
      coverPdfNumber={10}
    />
  );
};

export default NationalAccounts;
