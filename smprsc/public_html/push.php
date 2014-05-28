<?php
/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 *******************************************************************************/

/*
  This script receives POST from the applauncher and store the xml stub
  descriptions as xml files in a spoolDir

to test it from your webbrowser use following snipet

<form method="POST" action="push.php" enctype="multipart/form-data">
<input name="uid"/>
<input name="xmlSampStub"/>
<input type="submit"/>
</form>

*/

$spoolDir = "spool";

// return only simple messages
header("Content-Type: text/plain");

// Collect inputs
$uid          = basename($_POST['uid']);
$xmlSampStub = $_POST['xmlSampStub'];

// Try to validate xml string
libxml_use_internal_errors(true);
$xml = new DOMDocument();
$xml->loadXml($xmlSampStub);
if (!$xml->schemaValidate('SampStubDescriptor.xsd')) {
    echo "xml string not validated\n";
    header("HTTP/1.0 400 Bad Request");
    exit(1);
} else { 
    echo "validated\n";

}  

// Save as new file
$spoolFilename = $uid."-".time().".xml"; 
$spoolFile = fopen($spoolDir.'/'.$spoolFilename, 'w') or die("can't open file");
fwrite($spoolFile, $xmlSampStub);
fclose($spoolFile);
?>
