param (
[string]$Url
)

$a = [System.Net.WebUtility]::UrlEncode($Url) -replace '\+', '%20'
curl "http://localhost:8080?filepath=$a"

echo $a