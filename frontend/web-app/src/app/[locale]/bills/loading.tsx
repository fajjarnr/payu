export default function BillsLoading() {
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
          className="h-8 w-40 rounded-xl mb-2"
          style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
        />
        <div
          className="h-5 w-48 rounded-xl mb-8"
          style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
        />

        <div className="flex gap-3 mb-8 overflow-x-auto">
          {[...Array(5)].map((_, i) => (
            <div
              key={i}
              className="h-10 w-24 rounded-full shrink-0"
              style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
            />
          ))}
        </div>

        <div className="space-y-4">
          {[...Array(5)].map((_, i) => (
            <div
              key={i}
              className="flex items-center gap-4 p-4 rounded-2xl border"
              style={{
                borderColor: "var(--ant-color-border-secondary)",
                backgroundColor: "var(--ant-color-bg-container)",
              }}
            >
              <div
                className="w-12 h-12 rounded-xl"
                style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
              />
              <div className="flex-1">
                <div
                  className="h-4 w-40 rounded mb-1"
                  style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
                />
                <div
                  className="h-3 w-28 rounded"
                  style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
                />
              </div>
              <div className="text-right">
                <div
                  className="h-4 w-24 rounded mb-1"
                  style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
                />
                <div
                  className="h-8 w-16 rounded-full"
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
