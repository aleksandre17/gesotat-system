param(
  [Parameter(Mandatory=$true)][string]$InputDocx,
  [Parameter(Mandatory=$true)][string]$OutputPdf
)

$inputPath = (Resolve-Path -LiteralPath $InputDocx).Path
$outputPath = [System.IO.Path]::GetFullPath($OutputPdf)
$outputDirectory = [System.IO.Path]::GetDirectoryName($outputPath)
[System.IO.Directory]::CreateDirectory($outputDirectory) | Out-Null

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
try {
  $document = $word.Documents.Open($inputPath, $false, $false)
  try {
    $document.Repaginate()
    foreach ($field in $document.Fields) { $field.Update() | Out-Null }
    foreach ($toc in $document.TablesOfContents) { $toc.Update() | Out-Null }
    $document.Save()
    $wdExportFormatPDF = 17
    $wdExportOptimizeForPrint = 0
    $wdExportAllDocument = 0
    $wdExportDocumentContent = 0
    $wdExportCreateHeadingBookmarks = 1
    $document.ExportAsFixedFormat(
      $outputPath,
      $wdExportFormatPDF,
      $false,
      $wdExportOptimizeForPrint,
      $wdExportAllDocument,
      1,
      1,
      $wdExportDocumentContent,
      $true,
      $true,
      $wdExportCreateHeadingBookmarks,
      $true,
      $true,
      $false
    )
  }
  finally {
    $document.Close($false)
  }
}
finally {
  $word.Quit()
  [System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($word) | Out-Null
}

Write-Output $outputPath
