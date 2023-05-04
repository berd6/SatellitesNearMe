from AltAzRange import AltAzimuthRange
AltAzimuthRange.default_observer(51.773931, 18.061959, 50)
satellite_1 = AltAzimuthRange()
high_alt_balloon = AltAzimuthRange()
satellite_1.target(51.681562, 17.778988, 43152)
high_alt_balloon.target(52.30, 21.37, 190000)

satellite_1.calculate()
{'azimuth': 245.49, 'elevation': 86.86, 'distance': 430555.14}

high_alt_balloon.calculate()
{'azimuth': 74.1, 'elevation': 37.55, 'distance': 304391.38}