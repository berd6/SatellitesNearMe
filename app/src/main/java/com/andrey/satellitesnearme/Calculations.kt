package com.andrey.satellitesnearme

import kotlin.math.*

class Calculations (private val satellitesList: SatellitesModel, val myPosition: Point){

    private fun geocentricLatitude(lat: Double): Double {
        /*/ Преобразовать геодезическую широту 'lat' в геоцентрическую широту
            Геодезическая широта - это широта, заданная GPS.
            Геоцентрическая широта - это угол, измеренный от центра Земли между точкой и экватором.
            https://en.wikipedia.org/wiki/Latitude#Geocentric_latitude*/
        val e2 = 0.00669437999014
        return atan((1.0 - e2) * tan(lat))
    }

    private fun earthRadiusInMeters(latitude: Double): Double {
        val latitudeRadians= latitude * Math.PI / 180.0
        // // широта в радианах является геодезической, то есть такой, о которой сообщает GPS.
        // http://en.wikipedia.org/wiki/Earth_radius
        val a = 6378137.0  // экваториальный радиус в метрах (константа)
        val b = 6356752.3  // полярный радиус в метрах (константа)
        val cos = cos(latitudeRadians)
        val sin = sin(latitudeRadians)
        val t1 = a * a * cos
        val t2 = b * b * sin
        val t3 = a * cos
        val t4 = b * sin
        return sqrt((t1 * t1 + t2 * t2) / (t3 * t3 + t4 * t4))
    }


    data class PointLocation(
        val x: Double,
        val y: Double,
        val z: Double,
        val radius: Double,
        val nx: Double,
        val ny: Double,
        val nz: Double
    )

    private fun locationToPoint(p: Point): PointLocation {
        // Преобразовываем (lat, lon, lat) в (x, y, z).
        val lat = p.latitude * Math.PI / 180.0
        val lon = p.longitude * Math.PI / 180.0
        val radius = earthRadiusInMeters(lat)
        val clat = geocentricLatitude(lat)

        val cosLon = cos(lon)
        val sinLon = sin(lon)
        val cosLat = cos(clat)
        val sinLat = sin(clat)
        var x = radius * cosLon * cosLat
        var y = radius * sinLon * cosLat
        var z = radius * sinLat

        // Мы использовали геоцентрическую широту для вычисления (x, y, z) на земном эллипсоиде.
        // Теперь мы используем геодезическую широту для вычисления вектора нормали к поверхности с поправкой на высоту.
        val cosGlat = cos(lat)
        val sinGlat = sin(lat)

        val nx = cosGlat * cosLon
        val ny = cosGlat * sinLon
        val nz = sinGlat

        x += p.elevation * nx
        y += p.elevation * ny
        z += p.elevation * nz

        return PointLocation(x, y, z, radius, nx, ny, nz)
    }
    class Point(var latitude: Double, var longitude: Double, var elevation: Double, var radius: Double)

    private fun rotateGlobe(b: Point, a: Point): PointLocation {
// Получаем измененные координаты 'b', повернув глобус так, чтобы 'a' находился в широте =0, долготе=0.
        val br = Point(b.latitude, (b.longitude - a.longitude), b.elevation, b.radius)
        val brp = locationToPoint(br)

        /*/ Повернём декартовы координаты brp вокруг оси z на alon градусов,
            затем вокруг оси y на alat градусов.
            Хотя мы уменьшаемся на градус широты, как видно над осью y,
            это положительное вращение (против часовой стрелки) (если долгота B находится к востоку от долготы A).
            Однако с этой точки зрения ось x направлена влево.
            Итак, мы посмотрим в другую сторону, сделав ось x направленной вправо, ось z -
            направлен вверх, и вращение рассматривается как отрицательное.*/

        val alat = geocentricLatitude(-a.latitude * Math.PI / 180.0)
        val acos = cos(alat)
        val asin = sin(alat)

        val bx = (brp.x * acos) - (brp.z * asin)
        val by = brp.y
        val bz = (brp.x * asin) + (brp.z * acos)

        return PointLocation(
            x = bx,
            y = by,
            z = bz,
            radius = b.radius,
            nx = locationToPoint(b).nx,
            ny = locationToPoint(b).ny,
            nz = locationToPoint(b).nz
        )

    }

    private fun normalizeVectorDiff(a: PointLocation, b: PointLocation): PointLocation? {
// Вычислить norm(b-a), где norm делит вектор на его длину, чтобы получить единичный вектор.
        val dx = b.x - a.x
        val dy = b.y - a.y
        val dz = b.z - a.z
        val dist2 = dx * dx + dy * dy + dz * dz
        if (dist2 == 0.0) {
            return null
        }
        val dist = sqrt(dist2)
        return PointLocation((dx / dist), (dy / dist), (dz / dist), 1.0, 0.0, 0.0, 0.0)

    }

    data class AzAl(var azimuth: Double, var altitude: Double)

    private fun calculate(a: Point?, b: Point?): AzAl {
        val azAl = AzAl(0.0, 0.0)

        if (a != null && b != null) {

            val ap = locationToPoint(a)
            val bp = locationToPoint(b)


        /* Давайте воспользуемся хитростью для вычисления азимута:
           Повернём земной шар так, чтобы точка A выглядела как широта 0, долгота 0.
           Мы сохраняем фактические радиусы, рассчитанные на основе сплюснутого геоида,
           но используем углы, основанные на вычитании.
           Точка A будет находиться на x=радиус, y=0, z=0.
           Векторная разность B-A будет иметь составляющую dz = N/S , составляющую dy = E/W .*/
            val br = rotateGlobe(Point(b.latitude,b.longitude,b.elevation,earthRadiusInMeters(latitude = b.latitude)),
                Point(a.latitude,a.longitude,a.elevation,earthRadiusInMeters(latitude = a.latitude)))
            if (br.z * br.z + br.y * br.y > 1.0e-6) {
                val theta = atan2(br.z, br.y) * 180.0 / Math.PI
                var azimuth = 90.0 - theta
                if (azimuth < 0.0) {
                    azimuth += 360.0
                }
                if (azimuth > 360.0) {
                    azimuth -= 360.0
                }
                azAl.azimuth = azimuth
            }

            val bma = normalizeVectorDiff(bp, ap)
            if (bma != null) {
                //Вычислим высоту, которая представляет собой угол над горизонтом в точке B, если смотреть из точки A.

                val altitude = 90.0 - (180.0 / Math.PI) * acos(bma.x * ap.nx + bma.y * ap.ny + bma.z * ap.nz)
                azAl.altitude = altitude

            }


        }
        return azAl
    }

    fun completeSatelliteslist(): MutableList<SatellitesResults>{
        val satResults = MutableList(0) {SatellitesResults(satid=0, satname = "",0.0,0.0,0.0,0.0,0.0)}
        for (oneSatellite in satellitesList.above){
            val satellitePoint = Point(latitude = oneSatellite.satlat, longitude = oneSatellite.satlng, elevation = oneSatellite.satalt, radius = 0.0)
            val azAl: AzAl = calculate(myPosition, satellitePoint)
            val oneSatResult = SatellitesResults(
                satid = oneSatellite.satid,
                satname = oneSatellite.satname,
                satlat = oneSatellite.satlat,
                satlng = oneSatellite.satlng,
                satalt = oneSatellite.satalt,
                azimuth = azAl.azimuth,
                altitude = azAl.altitude)
            satResults.add(oneSatResult)

        }
        return satResults
    }
}


