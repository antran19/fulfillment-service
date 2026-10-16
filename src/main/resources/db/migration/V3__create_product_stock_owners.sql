-- Who owns a product's stock. Filled from the ProductCreated event (catalog-service is the
-- only service that knows the seller), so a Seller can later restock their own products
-- without fulfillment calling catalog. product_id is the PRODUCT id, the same value this
-- service stores in the sku_id columns (every product has exactly one SKU, see
-- commerce-service's StartCheckoutUseCase).
CREATE TABLE product_stock_owners (
    product_id VARCHAR(255) PRIMARY KEY,
    seller_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
