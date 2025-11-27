describe('Authentication Flow', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  it('should display login form', () => {
    cy.visit('/auth/login');
    cy.get('[data-cy="username-input"]', { timeout: 15000 }).should('be.visible');
    cy.get('[data-cy="password-input"]').should('be.visible');
    cy.get('[data-cy="login-button"]').should('be.visible');
  });

  it('should navigate to register page', () => {
    cy.visit('/auth/login');
    cy.get('[data-cy="register-link"]').click();
    cy.url().should('include', '/auth/register');
  });

  it('should show validation errors for empty form', () => {
    cy.visit('/auth/login');
    cy.get('[data-cy="login-button"]', { timeout: 15000 }).should('be.visible');
    cy.get('[data-cy="username-input"]').click().blur();
    cy.get('[data-cy="password-input"]').click().blur();
    cy.get('[data-cy="login-button"]').click({ force: true });
    cy.get('[data-cy="error-message"]', { timeout: 5000 }).should('have.length.at.least', 1);
  });
});

