$ErrorActionPreference = "Stop"

$projectId = "accomlink-1a5d6"
$apiKey = "AIzaSyCb-_mr-96Q-69eeUbAOAL6Tb3gO0UmBj4"
$baseUrl = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"
$now = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$dayMs = 24L * 60L * 60L * 1000L
$seedPassword = "12341234"

function ConvertTo-FirestoreValue {
    param([Parameter(Mandatory = $true)] $Value)

    if ($Value -is [bool]) {
        return @{ booleanValue = $Value }
    }
    if ($Value -is [int] -or $Value -is [long]) {
        return @{ integerValue = "$Value" }
    }
    if ($Value -is [double] -or $Value -is [decimal] -or $Value -is [float]) {
        return @{ doubleValue = [double]$Value }
    }
    if ($Value -is [array]) {
        return @{ arrayValue = @{ values = @($Value | ForEach-Object { ConvertTo-FirestoreValue $_ }) } }
    }
    return @{ stringValue = "$Value" }
}

function ConvertTo-FirestoreFields {
    param([Parameter(Mandatory = $true)] [hashtable] $Data)

    $fields = @{}
    foreach ($key in $Data.Keys) {
        $fields[$key] = ConvertTo-FirestoreValue $Data[$key]
    }
    return $fields
}

function Write-FirestoreDocument {
    param(
        [Parameter(Mandatory = $true)] [string] $Collection,
        [Parameter(Mandatory = $true)] [string] $DocumentId,
        [Parameter(Mandatory = $true)] [hashtable] $Data
    )

    $url = "$baseUrl/$Collection/$DocumentId`?key=$apiKey"
    $body = @{ fields = ConvertTo-FirestoreFields $Data } | ConvertTo-Json -Depth 12
    Invoke-RestMethod -Method Patch -Uri $url -ContentType "application/json" -Body $body | Out-Null
}

function Get-Sha256 {
    param([Parameter(Mandatory = $true)] [string] $Text)

    $bytes = [System.Text.Encoding]::UTF8.GetBytes($Text)
    $hash = [System.Security.Cryptography.SHA256]::Create().ComputeHash($bytes)
    return ($hash | ForEach-Object { $_.ToString("x2") }) -join ""
}

function Normalize-Email {
    param([Parameter(Mandatory = $true)] [string] $Email)
    return $Email.Trim().ToLowerInvariant()
}

function Get-PhoneSuffix {
    param(
        [Parameter(Mandatory = $true)] [int] $Base,
        [Parameter(Mandatory = $true)] [int] $Index
    )
    $suffix = ($Base + $Index).ToString()
    if ($suffix.Length -gt 6) {
        $suffix = $suffix.Substring($suffix.Length - 6)
    }
    return $suffix
}

$passwordHash = Get-Sha256 $seedPassword

$studentNames = @(
    "Kabelo Motsumi", "Naledi Dube", "Thabo Molefe", "Lerato Kgosi", "Neo Williams",
    "Amantle Ndlovu", "Tshepo Van Wyk", "Boitumelo Smith", "Mpho Moyo", "Katlego Brown",
    "Gaone Phiri", "Refilwe Johnson", "Tumelo Raditladi", "Kgomotso Adams", "Onalenna Sebina",
    "Palesa Morapedi", "Bakang Pretorius", "Gofaone Taylor", "Tebogo Mogapi", "Lesego Daniels",
    "Karabo Ramotswa", "Lorato Jacobs", "Olebile Mabiletsa", "Kedibone Campbell", "Bontle Moremi",
    "Aobakwe Williams", "Keitumetse Khan", "Omphile Cloete", "Tshiamo Ramatlhodi", "Atang Meyer",
    "Phenyo Sekgoma", "Dineo September", "Masego Kgafela", "Goitseone Botha", "Khumo Matlho",
    "Keneilwe Petersen", "Oarabile Moagi", "Rorisang Davids", "Kamogelo Motshwane", "Thatayaone Isaacs",
    "Botshelo Kgosidintsi", "Maitseo Ferreira", "Oratile Thema", "Wame Coetzee", "Bame Moloi",
    "Resego Swanepoel", "Tlotlo Makgato", "Kabo Olivier", "Reneilwe Charles", "Tumo Beukes"
)

$landlordNames = @(
    "MmaKgosi Letlole", "Tumelo Raditladi", "Grace Mooketsi", "Oaitse Ramorwa", "Kelebogile Moagi",
    "Patrick Molebatsi", "Dineo Seretse", "Mpho Letsholo", "Thato Kenosi", "Bontle Sebina"
)

$areas = @("Block 3", "Block 5", "Block 6", "Block 7", "Block 8", "Phase 2", "Phase 4", "Mogoditshane", "Tlokweng", "Gaborone West", "Broadhurst", "Extension 2", "Phakalane", "Fairgrounds", "Village")
$roomTypes = @("Single room", "Double room", "Studio", "Bedsitter", "1-bed flat", "2-bed flat", "Shared house", "Bachelor pad", "En-suite", "Self-contained unit")
$amenitiesPool = @("Wi-Fi", "Water", "Electricity", "Security")
$photos = @(
    "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2",
    "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267",
    "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85",
    "https://images.unsplash.com/photo-1484154218962-a197022b5858",
    "https://images.unsplash.com/photo-1493809842364-78817add7ffb"
)

for ($i = 0; $i -lt $studentNames.Count; $i++) {
    $name = $studentNames[$i]
    $parts = $name.ToLowerInvariant().Split(" ", [System.StringSplitOptions]::RemoveEmptyEntries)
    $email = "$($parts[0]).$($parts[$parts.Count - 1])@student.bac.bw"
    Write-FirestoreDocument "users" "student-$($i + 1)" @{
        name = $name
        email = $email
        emailNormalized = Normalize-Email $email
        phone = "71$(Get-PhoneSuffix 100000 $i)"
        role = "student"
        passwordHash = $passwordHash
        createdAt = $now - (($i + 1) * $dayMs)
    }
}

for ($i = 0; $i -lt $landlordNames.Count; $i++) {
    $name = $landlordNames[$i]
    $parts = $name.ToLowerInvariant().Split(" ", [System.StringSplitOptions]::RemoveEmptyEntries)
    $email = "$($parts[0]).$($parts[$parts.Count - 1])@landlord.accomlink.bw"
    Write-FirestoreDocument "users" "landlord-$($i + 1)" @{
        name = $name
        email = $email
        emailNormalized = Normalize-Email $email
        phone = "73$(Get-PhoneSuffix 200000 $i)"
        role = "landlord"
        passwordHash = $passwordHash
        createdAt = $now - (($i + 1) * 2L * $dayMs)
    }
}

for ($i = 0; $i -lt 50; $i++) {
    $area = $areas[$i % $areas.Count]
    $type = $roomTypes[$i % $roomTypes.Count]
    $price = [double](900 + (($i * 137) % 6600))
    $landlordIndex = $i % $landlordNames.Count
    $amenities = @()
    for ($j = 0; $j -lt 4; $j++) {
        $amenities += $amenitiesPool[($i + $j) % $amenitiesPool.Count]
    }
    $images = @(
        $photos[$i % $photos.Count],
        $photos[($i + 1) % $photos.Count]
    )

    Write-FirestoreDocument "listings" "seed-listing-$($i + 1)" @{
        landlordId = "landlord-$($landlordIndex + 1)"
        title = "$type in $area"
        description = "Comfortable student accommodation in $area with practical access to BAC routes and daily services."
        price = $price
        location = $area
        roomType = $type
        furnished = $false
        latitude = [double](-24.55 - ((($i * 37) % 200) / 1000.0))
        longitude = [double](25.85 + ((($i * 41) % 200) / 1000.0))
        amenities = $amenities
        images = $images
        isOccupied = ($i % 4 -eq 0)
        landlordPhone = "73$(Get-PhoneSuffix 200000 $landlordIndex)"
        availabilityDate = $now + (($i % 90) * $dayMs)
        depositAmount = $price
        createdAt = $now - (($i + 1) * 3600000L)
    }
}

Write-Host "Seed complete: 50 students (student-1..student-50), 10 landlords (landlord-1..landlord-10), and 50 listings written to $projectId."
Write-Host "All seeded users use password: $seedPassword"
