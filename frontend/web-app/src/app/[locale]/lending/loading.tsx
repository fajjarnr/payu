export default function LendingLoading() {
  return (
    <div
      className="min-h-screen animate-pulse"
      style={{ backgroundColor: "var(--ant-color-bg-layout)" }}
    >
      <div
        className="h-16 border-b"
        style={{
          borderColor: "var(--ant-color-border-secondary)",
          backgroundColor: "var(--ant-color-bg-container)",
        }}
      />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div
          className="h-8 w-32 rounded-xl mb-2"
          style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
        />
        <div
          className="h-5 w-52 rounded-xl mb-8"
          style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
        />

        <div
          className="p-6 rounded-2xl border mb-8"
          style={{
            borderColor: "var(--ant-color-border-secondary)",
            backgroundColor: "var(--ant-color-bg-container)",
          }}
        >
          <div
            className="h-4 w-28 rounded mb-3"
            style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
          />
          <div
            className="h-8 w-44 rounded-xl mb-4"
            style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
          />
          <div
            className="h-3 w-full rounded-full mb-2"
            style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
          />
          <div className="flex justify-between">
            <div
              className="h-3 w-20 rounded"
              style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
            />
            <div
              className="h-3 w-20 rounded"
              style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
            />
          </div>
        </div>

        <div className="grid md:grid-cols-2 gap-6">
          {[...Array(4)].map((_, i) => (
            <div
              key={i}
              className="p-6 rounded-2xl border"
              style={{
                borderColor: "var(--ant-color-border-secondary)",
                backgroundColor: "var(--ant-color-bg-container)",
              }}
            >
              <div
                className="h-5 w-36 rounded mb-2"
                style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
              />
              <div
                className="h-4 w-full rounded mb-4"
                style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
              />
              <div className="flex justify-between items-end">
                <div>
                  <div
                    className="h-3 w-16 rounded mb-1"
                    style={{
                      backgroundColor: "var(--ant-color-fill-secondary)",
                    }}
                  />
                  <div
                    className="h-6 w-24 rounded"
                    style={{
                      backgroundColor: "var(--ant-color-fill-secondary)",
                    }}
                  />
                </div>
                <div
                  className="h-10 w-24 rounded-full"
                  style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
                />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
