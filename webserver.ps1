# PowerShell simple HTTP server
$port = 8080
$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://localhost:$port/")

try {
    $listener.Start()
    Write-Output "Server started on port $port"
    while ($listener.IsListening) {
        $context = $listener.GetContext()
        $response = $null
        try {
            $request = $context.Request
            $response = $context.Response
            
            $urlPath = $request.Url.LocalPath
            if ($urlPath -eq "/") { $urlPath = "/test_calling.html" }
            
            $relPath = $urlPath.TrimStart('/')
            $filePath = Join-Path "C:\Users\SHADER7\.gemini\antigravity\scratch\S7" $relPath
            
            if (Test-Path $filePath -PathType Leaf) {
                [byte[]]$bytes = [System.IO.File]::ReadAllBytes($filePath)
                
                if ($filePath.EndsWith(".html")) { $response.ContentType = "text/html" }
                elseif ($filePath.EndsWith(".js")) { $response.ContentType = "application/javascript" }
                elseif ($filePath.EndsWith(".css")) { $response.ContentType = "text/css" }
                
                $response.ContentLength64 = $bytes.Length
                $response.OutputStream.Write($bytes, 0, $bytes.Length)
            } else {
                $response.StatusCode = 404
                [byte[]]$errBytes = [System.Text.Encoding]::UTF8.GetBytes("File Not Found: $urlPath")
                $response.ContentLength64 = $errBytes.Length
                $response.OutputStream.Write($errBytes, 0, $errBytes.Length)
            }
        } catch {
            Write-Output "Request error: $_"
        } finally {
            if ($null -ne $response) {
                try { $response.OutputStream.Close() } catch {}
            }
        }
    }
} catch {
    Write-Error $_
} finally {
    $listener.Stop()
}
