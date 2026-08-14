CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    order_number    VARCHAR(36)     NOT NULL UNIQUE,
    customer_name   VARCHAR(255)    NOT NULL,
    customer_email  VARCHAR(255)    NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    total_amount    NUMERIC(12, 2)  NOT NULL,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL
);

CREATE TABLE order_items (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT          NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_sku     VARCHAR(64)     NOT NULL,
    product_name    VARCHAR(255)    NOT NULL,
    quantity        INTEGER         NOT NULL,
    unit_price      NUMERIC(12, 2)  NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

CREATE TABLE inventory_reservations (
    id                  BIGSERIAL PRIMARY KEY,
    order_number        VARCHAR(36)     NOT NULL,
    product_sku         VARCHAR(64)     NOT NULL,
    quantity_reserved   INTEGER         NOT NULL,
    reserved_at         TIMESTAMP       NOT NULL
);

CREATE INDEX idx_inventory_reservations_sku ON inventory_reservations (product_sku);
CREATE INDEX idx_inventory_reservations_order_number ON inventory_reservations (order_number);
