<?php
/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 *******************************************************************************/


/* this script build one xml rss string with one item per file found into the
 * current directory 
 * Note: . .. and .* files are ignored 
 */

// Store spool directory name 
$spoolDir="spool";

// Start header to build xml string
$xml = '<?xml version="1.0" encoding="iso-8859-1"?><rss version="2.0">';
$xml .= '<channel>'; 
$xml .= '<title>File listing</title>';
$xml .= '<link>http://www.jmmc.fr/</link>';
$xml .= '<description>Show list of files</description>';

// walk through files of directory
foreach(glob($spoolDir."/*.*") as $filename){
    $file = basename ($filename);
    $titre=$file;
    $lien="";
    $date=date("D, d M Y H:i:s", filemtime($filename));
    $description=$file." stub pushed on ".$date;

    $xml .= '<item>';
    $xml .= '<title>'.$titre.'</title>';
    $xml .= '<link>'.$lien.'</link>';
    $xml .= '<pubDate>'.$date.' GMT</pubDate>'; 
    $xml .= '<description>'.$description.'</description>';
    $xml .= '</item>';  
}

// close remaining xml tags
$xml .= '</channel>';
$xml .= '</rss>';

echo "$xml";
?>
