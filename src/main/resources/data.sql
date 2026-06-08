INSERT INTO cities (name, state, country, latitude, longitude, population, area_km2, satellite_resolution, lidar_available, created_at, updated_at)
VALUES
    ('São Paulo',       'SP', 'Brasil', -23.5505, -46.6333, 12325232, 1521.11, '0.5m', true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Rio de Janeiro',  'RJ', 'Brasil', -22.9068, -43.1729, 6747815,  1200.27, '0.5m', true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Curitiba',        'PR', 'Brasil', -25.4290, -49.2671, 1948626,   435.27, '1m',   false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Belo Horizonte',  'MG', 'Brasil', -19.9208, -43.9378, 2530701,   330.99, '1m',   false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Manaus',          'AM', 'Brasil',  -3.1190, -60.0217, 2219580, 11401.09, '2m',   false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO simulations (simulation_type, status, parameters, results, risk_score, nasa_data_reference, climate_data, city_id, created_at, updated_at)
VALUES
    ('FLOOD',        'COMPLETED', '{"rainfallMm":150,"duration":"72h"}',
     '{"riskScore":0.82,"summary":"Risco crítico de enchente","recommendation":"ALTO RISCO - Ação imediata necessária"}',
     0.82, 'NASA_POWER|lat=-23.5505|lon=-46.6333', '{"T2M":22.5,"PRECTOTCORR":8.3}', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('TRAFFIC',      'COMPLETED', '{"peakHour":"08:00","vehicleCount":45000}',
     '{"riskScore":0.65,"summary":"Congestionamento moderado a severo","recommendation":"MÉDIO RISCO - Monitoramento recomendado"}',
     0.65, NULL, NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('CONSTRUCTION', 'PENDING',   '{"area":"Zona Norte","type":"Metrô","durationMonths":24}',
     NULL, NULL, NULL, NULL, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('FLOOD',        'PROCESSING','{"rainfallMm":200,"duration":"48h"}',
     NULL, NULL, NULL, NULL, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('HEAT_ISLAND',  'COMPLETED', '{"asphaltCoverage":0.78,"greenArea":0.12}',
     '{"riskScore":0.71,"summary":"Ilha de calor significativa detectada","recommendation":"MÉDIO RISCO - Monitoramento recomendado"}',
     0.71, 'NASA_POWER|lat=-19.9208|lon=-43.9378', '{"T2M":28.1,"RH2M":45.2}', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
