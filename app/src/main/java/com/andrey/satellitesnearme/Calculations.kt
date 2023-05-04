package com.andrey.satellitesnearme

import kotlin.math.*


//Переписал с JavaScript отсюда :https://javascript.plainenglish.io/calculating-azimuth-distance-and-altitude-from-a-pair-of-gps-locations-36b4325d8ab0
//Планируется проверить работу алгоритма уже после получения данных (https://doncross.net/geocalc/compass.html), а Elevation получать через Elevation API или Location.getAltitude() (если получиться получить местоположение)

class Calculations {

    fun geocentricLatitude(lat: Double): Double {
        /*/ Convert geodetic latitude 'lat' to a geocentric latitude 'clat'.
        // Geodetic latitude is the latitude as given by GPS.
        // Geocentric latitude is the angle measured from center of Earth between a point and the equator.
        // https://en.wikipedia.org/wiki/Latitude#Geocentric_latitude*/
        val e2 = 0.00669437999014;
        val clat = atan((1.0 - e2) * tan(lat));
        return clat;
    }

    fun earthRadiusInMeters(latitude: Double): Double {
        val latitudeRadians= latitude * Math.PI / 180.0
        // latitudeRadians is geodetic, i.e. that reported by GPS.
        // http://en.wikipedia.org/wiki/Earth_radius
        val a = 6378137.0;  // equatorial radius in meters
        val b = 6356752.3;  // polar radius in meters
        val cos = cos(latitudeRadians);
        val sin = sin(latitudeRadians);
        val t1 = a * a * cos;
        val t2 = b * b * sin;
        val t3 = a * cos;
        val t4 = b * sin;
        return sqrt((t1 * t1 + t2 * t2) / (t3 * t3 + t4 * t4));
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

    fun locationToPoint(p: Point): PointLocation {
        // Convert (lat, lon, elv) to (x, y, z).
        val lat = p.lat * Math.PI / 180.0;
        val lon = p.lon * Math.PI / 180.0;
        val radius = earthRadiusInMeters(lat);
        val clat = geocentricLatitude(lat);

        val cosLon = cos(lon);
        val sinLon = sin(lon);
        val cosLat = cos(clat);
        val sinLat = sin(clat);
        var x = radius * cosLon * cosLat;
        var y = radius * sinLon * cosLat;
        var z = radius * sinLat;

        // We used geocentric latitude to calculate (x,y,z) on the Earth's ellipsoid.
        // Now we use geodetic latitude to calculate normal vector from the surface, to correct for elevation.
        val cosGlat = cos(lat);
        val sinGlat = sin(lat);

        val nx = cosGlat * cosLon;
        val ny = cosGlat * sinLon;
        val nz = sinGlat;

        x += p.elev * nx;
        y += p.elev * ny;
        z += p.elev * nz;

        return PointLocation(x, y, z, radius, nx, ny, nz)
    }

    abstract class Point(var lat: Double, var lon: Double, var elev: Double)


    data class APoint(
        val latitude: Double,
        val longitude: Double,
        val elevation: Double,
        val radius: Double
    ) :
        Point(latitude, longitude, elevation)

    data class BPoint(
        var latitude: Double,
        var longitude: Double,
        var elevation: Double,
        var radius: Double
    ) :
        Point(latitude, longitude, elevation)


    //var a = APoint(22.0,22.0,1.0, earthRadiusInMeters(22.0))
    //var b = BPoint(20.0,27.0,100.0,earthRadiusInMeters(20.0))

    fun rotateGlobe(b: BPoint, a: APoint): PointLocation {
        // Get modified coordinates of 'b' by rotating the globe so that 'a' is at lat=0, lon=0.
        var br = BPoint(b.lat, (b.lon - a.lon), b.elevation, b.radius)
        var brp = locationToPoint(br);

        /*/ Rotate brp cartesian coordinates around the z-axis by a.lon degrees,
        // then around the y-axis by a.lat degrees.
        // Though we are decreasing by a.lat degrees, as seen above the y-axis,
        // this is a positive (counterclockwise) rotation (if B's longitude is east of A's).
        // However, from this point of view the x-axis is pointing left.
        // So we will look the other way making the x-axis pointing right, the z-axis
        // pointing up, and the rotation treated as negative.*/

        var alat = geocentricLatitude(-a.lat * Math.PI / 180.0);
        var acos = Math.cos(alat);
        var asin = Math.sin(alat);

        var bx = (brp.x * acos) - (brp.z * asin);
        var by = brp.y;
        var bz = (brp.x * asin) + (brp.z * acos);

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

    val pointA = APoint(22.0, 22.0, 1.0, earthRadiusInMeters(22.0))

    fun normalizeVectorDiff(a: PointLocation, b: PointLocation): PointLocation? {
        // Calculate norm(b-a), where norm divides a vector by its length to produce a unit vector.
        var dx = b.x - a.x;
        var dy = b.y - a.y;
        var dz = b.z - a.z;
        var dist2 = dx * dx + dy * dy + dz * dz;
        if (dist2 == 0.0) {
            return null;
        }
        var dist = Math.sqrt(dist2);
        return PointLocation((dx / dist), (dy / dist), (dz / dist), 1.0, 0.0, 0.0, 0.0)

    }

    data class AzAl(var azimuth: Double, var altitude: Double)

    fun Calculate(a: Point?, b: Point?): AzAl {
        var azAl = AzAl(0.0, 0.0)

        if (a != null && b != null) {

            if (b != null) {
                var ap = locationToPoint(a);
                var bp = locationToPoint(b);


                /* Let's use a trick to calculate azimuth:
                // Rotate the globe so that point A looks like latitude 0, longitude 0.
                // We keep the actual radii calculated based on the oblate geoid,
                // but use angles based on subtraction.
                // Point A will be at x=radius, y=0, z=0.
                // Vector difference B-A will have dz = N/S component, dy = E/W component.*/
                var br = rotateGlobe(BPoint(b.lat,b.lon,b.elev,earthRadiusInMeters(latitude = b.lat)),APoint(a.lat,a.lon,a.elev,earthRadiusInMeters(latitude = a.lat)));
                if (br.z * br.z + br.y * br.y > 1.0e-6) {
                    var theta = Math.atan2(br.z, br.y) * 180.0 / Math.PI;
                    var azimuth = 90.0 - theta;
                    if (azimuth < 0.0) {
                        azimuth += 360.0;
                    }
                    if (azimuth > 360.0) {
                        azimuth -= 360.0;
                    }
                    azAl.azimuth = azimuth
                }

                var bma = normalizeVectorDiff(bp, ap);
                if (bma != null) {
                    /*/ Calculate altitude, which is the angle above the horizon of B as seen from A.
                    // Almost always, B will actually be below the horizon, so the altitude will be negative.
                    // The dot product of bma and norm = cos(zenith_angle), and zenith_angle = (90 deg) - altitude.
                    // So altitude = 90 - acos(dotprod).*/
                    var altitude = 90.0 - (180.0 / Math.PI) * Math.acos(bma.x * ap.nx + bma.y * ap.ny + bma.z * ap.nz);
                    azAl.altitude = altitude

                }

            }


        }
        return azAl
    }
}


