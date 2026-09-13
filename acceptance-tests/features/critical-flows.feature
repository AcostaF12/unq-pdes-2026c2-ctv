@critical
Feature: Flujos críticos de Compra Tu Viaje
  Como equipo de desarrollo
  Queremos verificar los recorridos principales contra los servicios reales
  Para detectar fallos en la integración antes de publicar la aplicación

  Scenario: Un comprador inicia sesión correctamente
    Given existe el comprador inicial "buyer"
    When inicia sesión con la contraseña "buyer123"
    Then la autenticación responde con status 200
    And recibe un token para el rol "BUYER"

  Scenario: Una agencia publica un paquete válido
    Given inició sesión la agencia inicial
    And existen vuelos de ida y vuelta entre "BUE" y "PAR"
    When publica un paquete nuevo para "PAR" con precio 1750
    Then la publicación responde con status 201
    And el paquete queda asociado a la agencia "Despegar"

  Scenario: Un comprador encuentra un paquete por origen y destino
    Given existe un paquete nuevo entre "BUE" y "PAR"
    And inició sesión un comprador nuevo
    When busca paquetes desde "BUE" hacia "PAR"
    Then la búsqueda responde con status 200
    And el paquete nuevo aparece en los resultados

  Scenario: Un comprador compra un paquete con vuelos disponibles
    Given existe un paquete nuevo entre "BUE" y "PAR"
    And inició sesión un comprador nuevo
    When compra el paquete nuevo
    Then la compra responde con status 201
    And la compra figura en su historial con el precio publicado

  Scenario: Se compensa la venta de ida cuando no hay lugar en el regreso
    Given existe un paquete cuyo vuelo de regreso no tiene disponibilidad
    And inició sesión un comprador nuevo
    When intenta comprar el paquete nuevo
    Then la compra es rechazada con status 400
    And no queda una compra registrada
    And el vuelo de ida recupera su disponibilidad original

  Scenario: Un comprador no puede publicar paquetes
    Given inició sesión un comprador nuevo
    And existen vuelos de ida y vuelta entre "BUE" y "PAR"
    When el comprador intenta publicar un paquete para "PAR"
    Then la publicación es rechazada con status 403

  Scenario: Una compra reduce la disponibilidad de ambos vuelos
    Given existe un paquete nuevo entre "BUE" y "PAR"
    And inició sesión un comprador nuevo
    And se registra la disponibilidad actual de ambos vuelos
    When compra el paquete nuevo
    Then la compra responde con status 201
    And ambos vuelos tienen un asiento menos disponible
